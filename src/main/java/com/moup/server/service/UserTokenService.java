package com.moup.server.service;

import com.moup.server.model.entity.UserToken;
import com.moup.server.repository.UserTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;

    public void saveOrUpdateToken(Long userId, String refreshToken, Long refreshTokenExpiration) {
        // 유저 ID로 토큰이 존재하는지 확인
        Optional<UserToken> existingToken = userTokenRepository.findByUserId(userId);
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpiration);
        
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
                    .expiryDate(String.valueOf(expiryDate))
                    .build();
            userTokenRepository.save(newToken);
        }
    }

    public boolean isValidRefreshToken(Long userId, String refreshToken) {
        Optional<UserToken> existingToken = userTokenRepository.findByUserId(userId);

        if (existingToken.isPresent()) {
            UserToken userToken = existingToken.get();
            LocalDateTime expiryDate = LocalDateTime.parse(userToken.getExpiryDate());

            boolean isMatch = userToken.getRefreshToken().equals(refreshToken);
            boolean isExpired = expiryDate.isBefore(LocalDateTime.now());

            return isMatch && !isExpired;
        }
        return false;
    }
}
