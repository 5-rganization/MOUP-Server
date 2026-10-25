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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoogleAuthService extends BaseAuthService {

    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    @Value("${google.client.id}")
    private String googleClientId;
    @Value("${google.client.secret}")
    private String googleClientSecret;

    /// 요청마다 만들면 공개키 캐시가 매번 버려져 로그인할 때마다 Google 인증서를 다시 받는다(스코프 1 M10).
    /// 둘 다 스레드 안전하다.
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private GoogleIdTokenVerifier idTokenVerifier;

    public GoogleAuthService(SocialTokenRepository socialTokenRepository) {
        super(socialTokenRepository);
    }

    @PostConstruct
    void initVerifier() {
        idTokenVerifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
                .setAudience(Collections.singletonList(googleClientId)).build();
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
                    HTTP_TRANSPORT, JSON_FACTORY, googleClientId, googleClientSecret, authCode, "")
                    .execute();

            GoogleIdToken idToken = tokenResponse.parseIdToken();

            if (idToken == null || !idTokenVerifier.verify(idToken)) {
                throw new AuthException("ID 토큰 검증에 실패했습니다.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String userId = payload.getSubject();
            String name = (String) payload.get("name");

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            // 이미 동의한 계정이 다시 로그인·가입하면 Google이 refresh token을 주지 않는다는 가정을 실측하기 위한 로그다
            // (Phase 11 D11-6 ①). 없으면 이 계정은 탈퇴할 때 연동을 해제할 수 없다.
            log.info("Google 토큰 교환 - refresh token {}. sub={}", refreshToken != null ? "수신" : "없음", userId);

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
