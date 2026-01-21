package com.moup.global.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialTokenService {

    private final SocialTokenRepository socialTokenRepository;

    @Transactional
    public void saveOrUpdateToken(Long userId, String refreshToken) {
        // 유저 ID로 토큰이 존재하는지 확인
        Optional<SocialToken> existingToken = socialTokenRepository.findByUserId(userId);

        // 만약 기존에 토큰이 있으면, 갱신하기
        if (existingToken.isPresent()) {
            socialTokenRepository.updateById(existingToken.get().getId(), refreshToken);
        }
        // 만약 기존에 토큰이 없으면, 저장하기
        else {
            SocialToken newToken = SocialToken.builder()
                    .userId(userId)
                    .refreshToken(refreshToken)
                    .build();

            socialTokenRepository.save(newToken);
        }
    }
}
