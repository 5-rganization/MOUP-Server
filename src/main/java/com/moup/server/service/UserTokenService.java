package com.moup.server.service;

import com.moup.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;

    public void saveOrUpdateToken(Long userId, String refreshToken, Long refreshTokenExpiration) {
        // TODO: 토큰 갱신 로직 구현하기
        // 유저 ID로 토큰이 존재하는지 확인

    }
}
