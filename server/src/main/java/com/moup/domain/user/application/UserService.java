package com.moup.domain.user.application;

import com.moup.domain.user.dto.UserUpdateNicknameResponse;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.dto.UserCreateRequest;
import com.moup.domain.user.dto.UserDeleteResponse;
import com.moup.domain.user.dto.UserProfileImageResponse;
import com.moup.domain.user.dto.UserRegisterRequest;
import com.moup.domain.user.exception.UserAlreadyExistsException;
import com.moup.domain.user.exception.UserNotFoundException;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.infra.fcm.FCMTokenService;
import com.moup.global.infra.file.FileService;
import com.moup.global.security.token.SocialTokenService;
import com.moup.global.infra.s3.S3Service;
import com.moup.domain.auth.domain.Login;
import com.moup.global.common.type.Role;
import com.moup.global.error.AlreadyDeletedException;
import com.moup.global.error.InvalidArgumentException;
import com.moup.domain.auth.dto.LoginResponse;
import com.moup.domain.auth.dto.RegisterResponse;
import com.moup.global.security.token.TokenCreateRequest;
import com.moup.global.util.JwtUtil;
import com.moup.global.util.NameVerifyUtil;
import com.moup.global.security.token.UserTokenService;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final FileService fileService;
  private final S3Service s3Service;
  private final SocialTokenService socialTokenService;
  private final UserTokenService userTokenService;

  private final UserRepository userRepository;
  // 하드 삭제를 없애 users의 CASCADE가 더 이상 발화하지 않는다. 지울 것을 직접 지운다.
  private final com.moup.domain.alarm.mapper.AlarmRepository alarmRepository;
  private final com.moup.domain.routine.mapper.RoutineRepository routineRepository;
  private final com.moup.global.security.token.SocialTokenRepository socialTokenRepository;

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
      // Google은 최초 동의 시에만 refresh token을 발급하므로 재가입 시 null로 올 수 있다.
      // 로그인 분기(AuthController)와 동일하게 null을 허용한다.
      String socialRefreshToken = userCreateRequest.getSocialRefreshToken();
      if (socialRefreshToken != null && !socialRefreshToken.isEmpty()) {
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

    // 가입 시 스스로 부여할 수 있는 역할은 근무자/사장님뿐이다. ROLE_ADMIN은 허용하지 않는다.
    Role role = userRegisterRequest.getRole();
    if (role != Role.ROLE_WORKER && role != Role.ROLE_OWNER) {
      throw new InvalidArgumentException("허용되지 않는 역할입니다.");
    }

        String nickname = userRegisterRequest.getNickname();
        nameVerifyUtil.verifyNickname(nickname);

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

    // 이미지 타입인지 **파일 내용으로** 검증하고 저장에 쓸 확장자를 받는다.
    String extension = fileService.verifyImageAndResolveExtension(profileImage);

    // 업로드가 먼저다. 예전에는 기존 파일을 지운 뒤 업로드해서, 업로드가 실패하면
    // S3에 파일이 없는데 DB는 죽은 URL을 가리키는 상태로 남았다. 자가 복구가 안 된다.
    String imageUrl;
    try {
      imageUrl = s3Service.saveFile(profileImage, extension);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new FileUploadException("파일명 해싱 실패");
    }

    userRepository.updateProfileImg(userId, imageUrl);

    // 새 URL이 확정된 뒤에야 옛 파일을 지운다.
    // 삭제 실패는 고아 파일 하나로 끝나므로 요청을 실패시키지 않는다.
    String previousImage = user.getProfileImg();
    if (previousImage != null && !previousImage.equals(imageUrl)) {
      try {
        if (s3Service.doesFileExist(previousImage)) {
          s3Service.deleteFile(previousImage);
        }
      } catch (RuntimeException e) {
        log.warn("이전 프로필 이미지 삭제 실패 - 고아 파일이 남습니다. userId={}, url={}",
            userId, previousImage, e);
      }
    }

    return UserProfileImageResponse.builder().userId(userId).imageUrl(imageUrl).build();
  }

  @Transactional
  public UserDeleteResponse deleteUserSoftlyByUserId(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

    if (user.isDeleted()) {
      throw new AlreadyDeletedException();
    }

    userRepository.softDeleteUserById(userId);

    // 탈퇴 신청 즉시 refresh token을 폐기한다. 유예기간 동안 토큰이 살아 있으면
    // 계정 탈취 시 "탈퇴"라는 자구책이 무력화된다.
    userTokenService.deleteToken(userId);

    return UserDeleteResponse.builder()
        .userId(user.getId())
        .deletedAt(String.valueOf(LocalDateTime.now(SEOUL_ZONE_ID))) // 현재 시간을 직접 사용
        .isDeleted(true)
        .build();
  }

  /// 탈퇴 확정 — 행을 지우지 않고 **개인정보만 제거**한다 (확정 정책 5·7).
  ///
  /// 예전에는 `DELETE FROM users`였다. 그러면 `workplaces.owner_id`가 SET NULL이 되고
  /// 사장님이 만든 근무지에 남아 있던 알바생들의 근무·급여 이력이 함께 무너진다.
  /// 그 데이터는 사장님만의 것이 아니다 — 알바생의 임금·소득 증빙이기도 하다.
  ///
  /// 하드 삭제를 없앴으므로 `users`의 CASCADE가 더 이상 발화하지 않는다.
  /// **지워야 할 것을 직접 지운다.** 빠뜨리면 탈퇴자의 자격증명과 알림이 그대로 남는다.
  @Transactional
  public void anonymizeUserByUserId(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

    // 1. 개인 데이터 — 보존 대상이 아니다 (예전에는 CASCADE가 처리하던 것들)
    socialTokenRepository.deleteByUserId(userId);      // 소셜 자격증명
    userTokenService.deleteToken(userId);              // refresh 토큰
    fcmTokenService.deleteAllUserFCMTokens(userId);    // 푸시 토큰 = 기기 식별자
    routineRepository.deleteAllByUserId(userId);       // routine_tasks는 CASCADE로 따라감
    alarmRepository.deleteAllNormalAlarmByUserId(userId);
    alarmRepository.deleteAllAdminAlarmMappingsByUserId(userId);

    // 2. S3 프로필 이미지 — 예전 하드 삭제도 이걸 지우지 않아 객체가 영구히 남았다(기존 결함).
    //    삭제 실패로 탈퇴 처리를 되돌리지는 않는다. 고아 파일 하나가 낫다.
    String profileImg = user.getProfileImg();
    if (profileImg != null) {
      try {
        if (s3Service.doesFileExist(profileImg)) {
          s3Service.deleteFile(profileImg);
        }
      } catch (RuntimeException e) {
        log.warn("탈퇴 사용자 프로필 이미지 삭제 실패 - 고아 파일이 남습니다. userId={}, url={}",
            userId, profileImg, e);
      }
    }

    // 3. 보존: workplaces · workers · works · salaries
    userRepository.anonymizeUserById(userId);
  }

  @Transactional
  public void restoreUserByUserId(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    if (!user.isDeleted()) {
      throw new UserAlreadyExistsException();
    }
    // 가명처리가 끝났으면 되돌릴 것이 없다. 개인정보는 이미 제거됐고 provider_id도
    // 난수로 바뀌어 같은 소셜 계정으로 로그인하면 새 계정이 생긴다.
    // 여기서 막지 않으면 is_deleted만 0으로 돌아가 '이름 없는 유령 계정'이 살아난다.
    if (user.getAnonymizedAt() != null) {
      throw new AlreadyDeletedException();
    }

    userRepository.undeleteUserById(userId);
  }

  @Transactional
  public UserUpdateNicknameResponse updateNicknameByUserId(Long userId, String nickname) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    if (user.isDeleted()) {
      throw new AlreadyDeletedException();
    }

        nameVerifyUtil.verifyNickname(nickname);
        userRepository.updateNicknameById(userId, nickname);

    return UserUpdateNicknameResponse.builder()
        .userId(userId)
        .nickname(nickname)
        .build();
  }

  /// 로그아웃.
  ///
  /// `fcmToken`을 주면 **그 기기 하나만** 푸시를 끊는다. 예전에는 유저의 토큰 컬럼을
  /// 통째로 비워, 폰에서 로그아웃하면 태블릿 푸시까지 죽었다.
  /// 토큰을 모르면(구버전 클라이언트) 안전하게 전 기기를 끊는다.
  @Transactional
  public void logout(Long userId, String fcmToken) {
    // 1. 해당 기기의 FCM 토큰 제거
    fcmTokenService.deleteUserFCMToken(userId, fcmToken);

    // 2. refresh token 폐기. 이걸 지우지 않으면 로그아웃해도 최대 7일간 재발급이 가능하다.
    userTokenService.deleteToken(userId);
  }

  public void updateFCMTokenByUserId(Long userId, String fcmToken) {
    fcmTokenService.updateUserFCMToken(userId, fcmToken);
  }
}
