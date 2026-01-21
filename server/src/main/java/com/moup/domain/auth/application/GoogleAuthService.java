package com.moup.domain.auth.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moup.domain.auth.domain.Login;
import com.moup.global.security.token.SocialTokenRepository;
import jakarta.security.auth.message.AuthException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService extends BaseAuthService {

    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    @Value("${google.client.id}")
    private String googleClientId;
    @Value("${google.client.secret}")
    private String googleClientSecret;
    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    public GoogleAuthService(SocialTokenRepository socialTokenRepository) {
        super(socialTokenRepository);
    }

    @Override
    public Login getProvider() { return Login.LOGIN_GOOGLE; }

    @Override
    protected String getRevokeUrl() { return GOOGLE_REVOKE_URL; }

    @Override
    protected String buildRevokeRequestBody(String refreshToken) { return String.format("token=%s", refreshToken); }

    @Override
    public Map<String, Object> exchangeAuthCode(String authCode) throws AuthException {
        // 이 메서드는 Google Client Library를 사용하므로 기존 로직을 그대로 유지합니다.
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(), new GsonFactory(), googleClientId, googleClientSecret, authCode, "")
                    .execute();

            GoogleIdTokenVerifier idTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken idToken = tokenResponse.parseIdToken();

            if (idToken == null || !idTokenVerifier.verify(idToken)) {
                throw new AuthException("ID 토큰 검증에 실패했습니다.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String userId = payload.getSubject();
            String name = (String) payload.get("name");

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("name", name);
            userInfo.put("socialAccessToken", accessToken);
            userInfo.put("socialRefreshToken", refreshToken);

            return userInfo;
        } catch (IOException | GeneralSecurityException e) {
            throw new AuthException("Google 인증 코드 교환 또는 ID 토큰 검증에 실패했습니다.", e);
        }
    }
}
