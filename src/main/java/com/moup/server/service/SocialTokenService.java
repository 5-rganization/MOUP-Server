package com.moup.server.service;

import com.moup.server.model.entity.SocialToken;
import com.moup.server.repository.SocialTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SocialTokenService {

    SocialTokenRepository socialTokenRepository;

    public void saveOrUpdateToken(Long userId, String accessToken, String refreshToken) {
        // 유저 ID로 토큰이 존재하는지 확인
        Optional<SocialToken> existingToken = socialTokenRepository.findByUserId(userId);

        // 만약 기존에 토큰이 있으면, 갱신하기
        if (existingToken.isPresent()) {
            socialTokenRepository.updateById(existingToken.get().getId(), accessToken, refreshToken);
        }
        // 만약 기존에 토큰이 없으면, 저장하기
        else {
            SocialToken newToken = SocialToken.builder()
                    .userId(userId)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            socialTokenRepository.save(newToken);
        }
    }

}
