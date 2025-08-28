package com.moup.server.service;

import com.moup.server.common.File;
import com.moup.server.common.Login;
import com.moup.server.exception.AlreadyDeletedException;
import com.moup.server.exception.UserAlreadyExistsException;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import com.moup.server.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final FileService fileService;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final SocialTokenService socialTokenService;
    private final UserTokenService userTokenService;
    private final JwtUtil jwtUtil;

    @Transactional
    public RegisterResponse createUser(UserCreateRequest userCreateRequest) {
        try {
            Long userId = userRepository.create(userCreateRequest);

            // 1. 토큰 관리
            // 1-1. 소셜 토큰 관리
            String socialRefreshToken = userCreateRequest.getSocialRefreshToken();
            if (!socialRefreshToken.isEmpty()) {
                // Apple 로그인의 경우 Revoke를 위한 Social Refresh Token 저장
                socialTokenService.saveOrUpdateToken(userId, socialRefreshToken);
            }

            TokenCreateRequest tokenCreateRequest = TokenCreateRequest.builder().userId(userId).role(userCreateRequest.getRole()).username(userCreateRequest.getUsername()).build();

            // 1-2. 우리 서비스 토큰 관리
            String accessToken = jwtUtil.createAccessToken(tokenCreateRequest);
            String refreshToken = jwtUtil.createRefreshToken(tokenCreateRequest);
            userTokenService.saveOrUpdateToken(refreshToken, jwtUtil.getRefreshTokenExpiration());

            return RegisterResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
        } catch (DuplicateKeyException e) {
            throw new UserAlreadyExistsException();
        }
    }

    public User findByProviderAndId(Login provider, String providerId) {
        User user = userRepository.findByProviderAndId(provider, providerId).orElseThrow(UserNotFoundException::new);

        if (user.getIsDeleted()) {
            throw new AlreadyDeletedException();
        }

        return user;
    }

    public User findUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getIsDeleted()) {
            throw new AlreadyDeletedException();
        }

        return user;
    }

    public UserProfileImageResponse updateProfileImage(Long userId, MultipartFile profileImage) throws FileUploadException {
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

            return UserProfileImageResponse.builder().imageUrl(imageUrl).build();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new FileUploadException("파일명 해싱 실패");
        }
    }

    public UserDeleteResponse deleteSoftUserByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getIsDeleted()) {
            throw new AlreadyDeletedException();
        }

        userRepository.softDeleteUserById(userId);

        return UserDeleteResponse.builder().userId(user.getProviderId()).deletedAt(String.valueOf(LocalDateTime.now())) // 현재 시간을 직접 사용
                .isDeleted(true).build();
    }

    public UserRestoreResponse restoreUserByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (!user.getIsDeleted()) {
            throw new UserAlreadyExistsException();
        }

        userRepository.undeleteUserById(userId);

        return UserRestoreResponse.builder().userId(user.getProviderId()).deletedAt(null).isDeleted(false).build();
    }
}
