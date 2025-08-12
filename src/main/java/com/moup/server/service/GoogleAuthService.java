package com.moup.server.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.entity.User;
import jakarta.security.auth.message.AuthException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService implements AuthService {

    @Value("${google.client.id}")
    private String googleClientId;
    @Value("${google.client.secret}")
    private String googleClientSecret;
    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    @Override
    public Login getProvider() {
        return Login.LOGIN_GOOGLE;
    }

    @Override
    public Map<String, Object> exchangeAuthCode(String authCode) throws AuthException {
        try {
            // 1. 소셜 OAuth 서버로 토큰 교환 요청
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(),
                    new GsonFactory(), googleClientId, googleClientSecret, authCode, googleRedirectUri).execute();

            // 2. ID 토큰 검증
            GoogleIdTokenVerifier idTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory()).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken idToken = tokenResponse.parseIdToken();

            if (idToken == null || !idTokenVerifier.verify(idToken)) {
                throw new AuthException("ID 토큰 검증에 실패했습니다.");
            }

            // 3. 사용자 정보 추출
            GoogleIdToken.Payload payload = idToken.getPayload();
            String userId = payload.getSubject();
            String name = (String) payload.get("name");

            // 4. 소셜 토큰 정보 추출 및 반환
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("name", name);
            userInfo.put("socialAccessToken", accessToken);
            userInfo.put("socialRefreshToken", refreshToken);

            return userInfo;
        } catch (IOException | GeneralSecurityException e) {
            throw new AuthException("인증 코드 교환 또는 ID 토큰 검증 실패.");
        }
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return (String) userInfo.get("userId");
    }

    @Override
    public String getUsername(Map<String, Object> userInfo) {
        return (String) userInfo.get("name");
    }
}
