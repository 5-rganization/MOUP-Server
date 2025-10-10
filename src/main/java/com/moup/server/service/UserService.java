package com.moup.server.service;

import com.moup.server.common.File;
import com.moup.server.common.Login;
import com.moup.server.common.Role;
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
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private final FileService fileService;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final SocialTokenService socialTokenService;
    private final UserTokenService userTokenService;
    private final JwtUtil jwtUtil;

    private static final Pattern CONSONANTS_ONLY_PATTERN = Pattern.compile("^[ㄱ-ㅎ]+$");
    private static final Pattern VOWELS_ONLY_PATTERN = Pattern.compile("^[ㅏ-ㅣ]+$");
    private static final Pattern INCOMPLETE_HANGUL_PATTERN = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ]");
    private static final Pattern HANGUL_PATTERN = Pattern.compile("[가-힣]");
    private static final Pattern ALPHABET_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^가-힣a-zA-Z0-9]");

    @Transactional
    public LoginResponse startCreateUser(UserCreateRequest userCreateRequest) {
        try {
            userRepository.create(userCreateRequest);
            Long userId = userCreateRequest.getUserId();

            // 1. 토큰 관리
            // 1-1. 소셜 토큰 관리
            String socialRefreshToken = userCreateRequest.getSocialRefreshToken();
            if (!socialRefreshToken.isEmpty()) {
                // Apple 로그인의 경우 Revoke를 위한 Social Refresh Token 저장
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

            return LoginResponse.builder()
                    .userId(userId)
                    .role(null)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (DuplicateKeyException e) {
            throw new UserAlreadyExistsException();
        }
    }

    public RegisterResponse completeCreateUser(UserRegisterRequest userRegisterRequest) {
        Long userId = userRegisterRequest.getUserId();
        User userToUpdate = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (userToUpdate.isRegisterCompleted()) { throw new UserAlreadyExistsException(); }

        String nickname = userRegisterRequest.getNickname();
        validateNickname(nickname);

        userRepository.updateById(userId, userRegisterRequest.getNickname(), userRegisterRequest.getRole(), true);
        return RegisterResponse.builder()
                .userId(userId)
                .role(userRegisterRequest.getRole())
                .build();
    }

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

            return UserProfileImageResponse.builder().userId(userId).imageUrl(imageUrl).build();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new FileUploadException("파일명 해싱 실패");
        }
    }

    public UserDeleteResponse deleteSoftUserByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.isDeleted()) {
            throw new AlreadyDeletedException();
        }

        userRepository.softDeleteUserById(userId);

        return UserDeleteResponse.builder().userId(user.getId()).deletedAt(String.valueOf(LocalDateTime.now())) // 현재 시간을 직접 사용
                .isDeleted(true).build();
    }

    public void restoreUserByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!user.isDeleted()) { throw new UserAlreadyExistsException(); }

        userRepository.undeleteUserById(userId);
    }

    public UserUpdateNicknameResponse updateNicknameByUserId(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!user.isDeleted()) { throw new AlreadyDeletedException(); }

        validateNickname(nickname);
        userRepository.updateNicknameById(userId, nickname);

        return UserUpdateNicknameResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .build();
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("한글, 영문 또는 숫자만 사용하여 8자 이하로 입력해주세요");
        }

        String trimmed = nickname.trim();

        if (!nickname.equals(trimmed) || nickname.contains(" ")) {
            throw new IllegalArgumentException("닉네임 앞뒤 또는 중간에 공백을 사용할 수 없어요");
        }

        // 👇 미리 컴파일된 패턴 사용
        if (CONSONANTS_ONLY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("자음만 사용할 수 없어요");
        }

        if (VOWELS_ONLY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("모음만 사용할 수 없어요");
        }

        if (INCOMPLETE_HANGUL_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("정확한 글자를 입력해주세요");
        }

        boolean containsHangul = HANGUL_PATTERN.matcher(trimmed).find();
        boolean containsAlphabet = ALPHABET_PATTERN.matcher(trimmed).find();
        if (containsHangul && containsAlphabet) {
            throw new IllegalArgumentException("한글 또는 영문만 사용할 수 있어요");
        }

        if (SPECIAL_CHAR_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("특수문자는 사용할 수 없어요");
        }

        if (trimmed.length() > 8) {
            throw new IllegalArgumentException("8자 이하로 입력해주세요");
        }
    }
}
