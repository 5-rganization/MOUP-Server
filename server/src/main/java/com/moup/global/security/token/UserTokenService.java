package com.moup.global.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import com.moup.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class UserTokenService {

    private final JwtUtil  jwtUtil;
    private final UserTokenRepository userTokenRepository;

    @Transactional
    public void saveOrUpdateToken(String refreshToken, Long refreshTokenExpiration) {
        Long userId = jwtUtil.getUserId(refreshToken);

        // 유저 ID로 토큰이 존재하는지 확인
        Optional<UserToken> existingToken = userTokenRepository.findByUserId(userId);
        LocalDateTime expiryDate = LocalDateTime.now(SEOUL_ZONE_ID).plusSeconds(refreshTokenExpiration / 1000);
        String tokenHash = hash(refreshToken);
        
        // 만약 기존에 토큰이 있으면, 갱신하기
        if (existingToken.isPresent()) {
            // 이상 탐지?
            userTokenRepository.updateById(existingToken.get().getId(), tokenHash, String.valueOf(expiryDate));
        }
        // 만약 기존에 토큰이 없으면, 저장하기
        else {
            UserToken newToken = UserToken.builder()
                    .userId(userId)
                    .refreshToken(tokenHash)
                    .expiryDate(expiryDate)
                    .build();
            userTokenRepository.save(newToken);
        }
    }

    /// 해당 유저의 refresh token을 폐기한다. 로그아웃·탈퇴 신청 시 호출한다.
    @Transactional
    public void deleteToken(Long userId) {
        userTokenRepository.deleteByUserId(userId);
    }

    /// refresh token은 **해시로만** 저장한다.
    ///
    /// 이 토큰은 유효기간 7일짜리 전권 크리덴셜이라, 평문으로 두면
    /// **DB 읽기 권한만으로 전 사용자 계정에 로그인할 수 있다.**
    /// 검증이 문자열 비교 한 줄이라 해시로 바꾸는 비용이 거의 없다.
    ///
    /// JWT는 이미 고엔트로피라 salt·work factor가 필요 없다. 세션 토큰 저장과 같은 이유다.
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 제공한다. 여기 오면 런타임이 깨진 것이다.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }

    public boolean isValidRefreshToken(String refreshToken) {
        // 서명·만료·용도를 먼저 확인한다. 이 가드가 없으면 access token으로 재발급을 받을 수
        // 있고, 만료·변조 토큰이 아래 getUserId에서 예외를 던져 500이 된다.
        if (!jwtUtil.isValidRefreshTokenType(refreshToken)) {
            return false;
        }

        Long userId = jwtUtil.getUserId(refreshToken);

        Optional<UserToken> existingToken = userTokenRepository.findByUserId(userId);

        if (existingToken.isPresent()) {
            UserToken userToken = existingToken.get();
            LocalDateTime expiryDate = userToken.getExpiryDate();

            boolean isMatch = MessageDigest.isEqual(
                    userToken.getRefreshToken().getBytes(StandardCharsets.UTF_8),
                    hash(refreshToken).getBytes(StandardCharsets.UTF_8));
            boolean isExpired = expiryDate.isBefore(LocalDateTime.now(SEOUL_ZONE_ID));

            return isMatch && !isExpired;
        }
        return false;
    }
}
