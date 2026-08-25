package com.moup.global.security.token;

import java.time.LocalDateTime;
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
        System.out.println(expiryDate);
        
        // 만약 기존에 토큰이 있으면, 갱신하기
        if (existingToken.isPresent()) {
            // 이상 탐지?
            userTokenRepository.updateById(existingToken.get().getId(), refreshToken, String.valueOf(expiryDate));
        }
        // 만약 기존에 토큰이 없으면, 저장하기
        else {
            UserToken newToken = UserToken.builder()
                    .userId(userId)
                    .refreshToken(refreshToken)
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

            boolean isMatch = userToken.getRefreshToken().equals(refreshToken);
            boolean isExpired = expiryDate.isBefore(LocalDateTime.now(SEOUL_ZONE_ID));

            return isMatch && !isExpired;
        }
        return false;
    }
}
