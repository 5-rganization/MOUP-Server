package com.moup.server.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
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
    public Map<String, Object> exchangeAuthCode(String authCode) throws InvalidTokenException {
        
        try {
            // 1. 소셜 OAuth 서버로 토큰 교환 요청
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(), new GsonFactory(),
                    googleClientId, googleClientSecret,
                    authCode, googleRedirectUri)
                    .execute();

            // 2. ID 토큰 검증
            GoogleIdTokenVerifier idTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory()).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken idToken = tokenResponse.parseIdToken();
            if (idToken == null || idTokenVerifier.verify(idToken)) {
                throw new InvalidTokenException();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);  // Auth Code 교환 요청 실패
        } catch (GeneralSecurityException e) {
            throw new InvalidTokenException();
        }

        // 3. 토큰 관리

        // 4. 사용자 정보 추출

        Map<String, Object> userInfo = new HashMap<>();
        return userInfo;
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return "";
    }

    @Override
    public String getUsername(Map<String, Object> userInfo) {
        return "";
    }

    public Map<String, Object> verifyIdToken(String idTokenString) throws InvalidTokenException {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                new GsonFactory()).setAudience(Collections.singletonList(googleClientId)).build();

        GoogleIdToken idToken = null;
        try {
            idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // ID 토큰에서 사용자 정보 추출
                String userId = payload.getSubject();
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String locale = (String) payload.get("locale");
                String familyName = (String) payload.get("familyName");
                String givenName = (String) payload.get("givenName");

                return Map.of("userId", userId, "name", name);
            } else {
                throw new InvalidTokenException();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidTokenException();
        }

    }
}
