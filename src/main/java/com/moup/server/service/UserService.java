package com.moup.server.service;

import com.moup.server.common.File;
import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.exception.AlreadyDeletedException;
import com.moup.server.exception.UserAlreadyExistsException;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.LoginResponse;
import com.moup.server.model.dto.RegisterResponse;
import com.moup.server.model.dto.TokenCreateRequest;
import com.moup.server.model.dto.UserCreateRequest;
import com.moup.server.model.dto.UserDeleteResponse;
import com.moup.server.model.dto.UserProfileImageResponse;
import com.moup.server.model.dto.UserRegisterRequest;
import com.moup.server.model.dto.UserUpdateNicknameResponse;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import com.moup.server.util.JwtUtil;
import com.moup.server.util.NameVerifyUtil;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

  private final FileService fileService;
  private final S3Service s3Service;
  private final SocialTokenService socialTokenService;
  private final UserTokenService userTokenService;

  private final UserRepository userRepository;

  private final NameVerifyUtil nameVerifyUtil;
  private final JwtUtil jwtUtil;
  private final FCMTokenService fcmTokenService;

  @Transactional
  public LoginResponse startCreateUser(UserCreateRequest userCreateRequest) {
    try {
      userRepository.create(userCreateRequest);
      Long userId = userCreateRequest.getUserId();

      // 1. 토큰 관리
      // 1-1. 소셜 토큰 관리
      String socialRefreshToken = userCreateRequest.getSocialRefreshToken();
      if (!socialRefreshToken.isEmpty()) {
        // Revoke를 위한 Social Refresh Token 저장
        socialTokenService.saveOrUpdateToken(userId, socialRefreshToken);
      }

      TokenCreateRequest tokenCreateRequest = TokenCreateRequest.builder()
          .userId(userId)
          .role(Role.ROLE_WORKER)  // SQL role 기본값
          .username(userCreateRequest.getUsername())
          .build();

      // 1-2. 우리 서비스 토큰 관리
      String accessToken = jwtUtil.createAccessToken(tokenCreateRequest);
      String refreshToken = jwtUtil.createRefreshToken(tokenCreateRequest);
      userTokenService.saveOrUpdateToken(refreshToken, jwtUtil.getRefreshTokenExpiration());

      // 1-3. FCM 토큰 관리
      fcmTokenService.updateUserFCMToken(userId, userCreateRequest.getFcmToken());

      return LoginResponse.builder()
          .role(null)
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .build();
    } catch (DuplicateKeyException e) {
      throw new UserAlreadyExistsException();
    }
  }

  @Transactional
  public RegisterResponse completeCreateUser(UserRegisterRequest userRegisterRequest) {
    Long userId = userRegisterRequest.getUserId();
    User userToUpdate = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    if (userToUpdate.getNickname() != null) {
      throw new UserAlreadyExistsException();
    }

    String nickname = userRegisterRequest.getNickname();
    nameVerifyUtil.validateNickname(nickname);

    userRepository.updateById(userId, userRegisterRequest.getNickname(),
        userRegisterRequest.getRole());
    return RegisterResponse.builder()
        .role(userRegisterRequest.getRole())
        .build();
  }

  @Transactional(readOnly = true)
  public Optional<User> findByProviderAndId(Login provider, String providerId) {
    return userRepository.findByProviderAndId(provider, providerId);
  }

  public User findUserById(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

    if (user.isDeleted()) {
      throw new AlreadyDeletedException();
    }

    return user;
  }

  @Transactional
  public UserProfileImageResponse updateProfileImage(Long userId, MultipartFile profileImage)
      throws FileUploadException {
    User user = findUserById(userId);

    // 이미지 타입인지 파일 검증
    fileService.verifyFileExtension(profileImage, File.IMAGE);

    // 기존 이미지가 있다면 해당 파일 삭제
    if (user.getProfileImg() != null && s3Service.doesFileExist(user.getProfileImg())) {
      s3Service.deleteFile(user.getProfileImg());
    }

    // 새 이미지 업로드하기
    try {
      String imageUrl = s3Service.saveFile(profileImage);
      userRepository.updateProfileImg(userId, imageUrl);

      return UserProfileImageResponse.builder().userId(userId).imageUrl(imageUrl).build();
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new FileUploadException("파일명 해싱 실패");
    }
  }

  @Transactional
  public UserDeleteResponse deleteUserSoftlyByUserId(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

    if (user.isDeleted()) {
      throw new AlreadyDeletedException();
    }

    userRepository.softDeleteUserById(userId);

    return UserDeleteResponse.builder()
        .userId(user.getId())
        .deletedAt(String.valueOf(LocalDateTime.now())) // 현재 시간을 직접 사용
        .isDeleted(true)
        .build();
  }

  @Transactional
  public void deleteUserHardlyByUserId(Long userId) {
    userRepository.hardDeleteUserById(userId);
  }

  @Transactional
  public void restoreUserByUserId(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    if (!user.isDeleted()) {
      throw new UserAlreadyExistsException();
    }

    userRepository.undeleteUserById(userId);
  }

  @Transactional
  public UserUpdateNicknameResponse updateNicknameByUserId(Long userId, String nickname) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    if (user.isDeleted()) {
      throw new AlreadyDeletedException();
    }

    nameVerifyUtil.validateNickname(nickname);
    userRepository.updateNicknameById(userId, nickname);

    return UserUpdateNicknameResponse.builder()
        .userId(userId)
        .nickname(nickname)
        .build();
  }

  @Transactional
  public void logout(Long userId) {
    // 1. FCM 토큰 초기화
    fcmTokenService.deleteUserFCMToken(userId);
  }

  public void updateFCMTokenByUserId(Long userId, String fcmToken) {

    

    fcmTokenService.updateUserFCMToken(userId, fcmToken);
  }
}
