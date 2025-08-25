package com.moup.server.service;

import com.google.gson.Gson;
import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;
import com.moup.server.util.AppleJwtUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.security.auth.message.AuthException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppleAuthService implements AuthService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";

    @Value("${apple.client.id}")
    private String appleClientId;
    @Value("${apple.team.id}")
    private String appleTeamId;
    @Value("${apple.key.id}")
    private String appleKeyId;
    @Value("${apple.private.key}")
    private String applePrivateKey;
    @Value("${apple.redirect.uri}")
    private String appleRedirectUri;

    private final JWKSource<SecurityContext> jwkSet;
    private final AppleJwtUtil appleJwtUtil;

    public AppleAuthService() throws MalformedURLException {
        this.jwkSet = JWKSourceBuilder.create(new URL(APPLE_JWKS_URL)).build();
        this.appleJwtUtil = new AppleJwtUtil(appleTeamId, appleKeyId, applePrivateKey);
    }

    @Override
    public Login getProvider() {
        return Login.LOGIN_APPLE;
    }

    @Override
    public Map<String, Object> exchangeAuthCode(String authCode) throws AuthException {

        try {
            // 1. JWT 형식의 Client Secret 동적 생성
            String clientSecret = appleJwtUtil.createClientSecret();

            // 2. HTTP 요청으로 토큰 교환
            URL url = new URL(APPLE_TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String postData = String.format("client_id=%s&client_secret=%s&code=%s&grant_type=%s&redirect_uri=%s",
                    appleClientId, clientSecret, authCode, "authorization_code", appleRedirectUri);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 토큰 교환 실패 시 오류 메시지 읽기
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    throw new AuthException("Apple Auth Code 교환 실패: " + response);
                }
            }

            // HTTP 응답 파싱 및 토큰 추출
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                // 3. 응답에서 ID 토큰, Access Token, Refresh Token 추출
                Gson gson = new Gson();
                Map<String, Object> tokenResponse = gson.fromJson(response.toString(), Map.class);
                String idTokenString = (String) tokenResponse.get("id_token");
                String socialAccessToken = (String) tokenResponse.get("access_token");
                String socialRefreshToken = (String) tokenResponse.get("refresh_token");

                // 4. ID 토큰 검증
                ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSet);
                jwtProcessor.setJWSKeySelector(keySelector);

                JWTClaimsSet exactMatchClaims = new JWTClaimsSet.Builder().issuer("https://appleid.apple.com").build();
                Set<String> requiredClaims = new HashSet<>();
                requiredClaims.add("exp");
                requiredClaims.add("iat");

                jwtProcessor.setJWTClaimsSetVerifier(
                        new DefaultJWTClaimsVerifier<>(
                                new HashSet<>(Collections.singletonList(appleClientId)),
                                exactMatchClaims,
                                requiredClaims,
                                null
                        )
                );

                JWTClaimsSet claimsSet = jwtProcessor.process(idTokenString, null);

                // 5. 사용자 정보 추출
                String userId = claimsSet.getSubject();
                String email = claimsSet.getStringClaim("email");

                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", userId);
                userInfo.put("email", email);
                userInfo.put("socialAccessToken", socialAccessToken);
                userInfo.put("socialRefreshToken", socialRefreshToken);
                return userInfo;
            }

        } catch (JOSEException | ParseException | BadJOSEException | IOException | GeneralSecurityException e) {
            throw new AuthException("Apple 인증 코드 교환 또는 ID 토큰 검증 실패.", e);
        }
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return (String) userInfo.get("userId");
    }

    @Override
    public String getUsername(Map<String, Object> userInfo) {
        return null;
    }
}
