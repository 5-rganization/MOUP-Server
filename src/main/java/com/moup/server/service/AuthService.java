package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface AuthService {
    /**
     * 소셜 로그인 제공자 유형을 반환합니다.
     * @return Login provider (ex: LOGIN_GOOGLE, LOGIN_APPLE)
     */
    Login getProvider();

    /**
     *  Authroization Code를 받아 토큰 교환 및 사용자 정보를 처리합니다.
     * @param authCode
     * @return 사용자 정보와 토큰을 포함하는 Map
     */
    Map<String, Object> exchangeAuthCode(String authCode) throws InvalidTokenException;

    /**
     * 처리된 사용자 정보 맵에서 제공자 별 사용자 고유 ID를 반환합니다.
     * @param userInfo
     * @return String userId
     */
    String getProviderId(Map<String, Object> userInfo);

    /**
     * 처리된 사용자 정보 맵에서 사용자 이름을 반환합니다.
     * @param userInfo
     * @return String username
     */
    String getUsername(Map<String, Object> userInfo);
}
