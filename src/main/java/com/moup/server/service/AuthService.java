package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;

import jakarta.security.auth.message.AuthException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface AuthService {
    /// 소셜 로그인 제공자 유형을 반환합니다.
    /// @return Login provider (LOGIN_GOOGLE, LOGIN_APPLE, LOGIN_NAVER, LOGIN_KAKAO)
    Login getProvider();

    ///  Authorization Code를 받아 토큰 교환 및 사용자 정보를 처리합니다.
    /// @param authCode OAuth 2.0 Authorization Code
    /// @return 사용자 정보와 토큰을 포함하는 Map
    Map<String, Object> exchangeAuthCode(String authCode) throws AuthException;

    /// 처리된 사용자 정보 맵에서 제공자 별 사용자 고유 ID를 반환합니다.
    /// @param userInfo 사용자 정보 맵
    /// @return String userId
    String getProviderId(Map<String, Object> userInfo);

    /// 처리된 사용자 정보 맵에서 사용자 이름을 반환합니다.
    /// @param userInfo 사용자 정보 맵
    /// @return String username
    String getUsername(Map<String, Object> userInfo);

    /// 소셜 로그인의 연결을 끊습니다.
    /// @param userId 사용자 ID
    /// @throws AuthException Authorization Code 혹은 토큰 검증 실패
    void revokeToken(Long userId) throws AuthException;
}
