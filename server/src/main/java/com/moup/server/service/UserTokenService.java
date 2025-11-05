package com.moup.server.service;

import com.moup.server.model.entity.UserToken;
import com.moup.server.repository.UserTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;

import com.moup.server.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.server.common.TimeConstants.SEOUL_ZONE_ID;

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

    public boolean isValidRefreshToken(String refreshToken) {
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
