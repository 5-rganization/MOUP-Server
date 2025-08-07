package com.moup.server.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService implements AuthService {

    @Value("${google.client.id}")
    private String googleClientId;

    @Override
    public Login getProvider() {
        return Login.LOGIN_GOOGLE;
    }

    @Override
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
