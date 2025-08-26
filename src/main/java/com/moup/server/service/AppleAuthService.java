package com.moup.server.service;

import com.google.gson.Gson;
import com.moup.server.common.Login;
import com.moup.server.model.entity.SocialToken;
import com.moup.server.repository.SocialTokenRepository;
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
import java.util.*;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AppleAuthService implements AuthService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    @Value("${apple.client.id}")
    private String appleClientId;
    @Value("${apple.redirect.uri}")
    private String appleRedirectUri;

    private final JWKSource<SecurityContext> jwkSet;
    private final AppleJwtUtil appleJwtUtil;
    private final SocialTokenRepository socialTokenRepository;

    public AppleAuthService(@Value("${apple.team.id}") String appleTeamId,
                            @Value("${apple.key.id}") String appleKeyId,
                            @Value("${apple.private.key}") String applePrivateKey,
                            @Value("${apple.client.id}") String appleClientId,
                            SocialTokenRepository socialTokenRepository) throws MalformedURLException {
        // @Value로 주입받는 값들을 사용해 필드 초기화
        this.appleJwtUtil = new AppleJwtUtil(appleTeamId, appleKeyId, applePrivateKey, appleClientId);
        // jwkSet 초기화
        this.jwkSet = JWKSourceBuilder.create(new URL(APPLE_JWKS_URL)).build();
        // Repository 주입
        this.socialTokenRepository = socialTokenRepository;
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

    @Async
    public void revokeToken(Long userId) throws AuthException {
        // 1. DB에서 해당 유저의 refreshToken 조회
        Optional<SocialToken> socialTokenOptional = socialTokenRepository.findByUserId(userId);

        if (socialTokenOptional.isPresent()) {
            SocialToken socialToken = socialTokenOptional.get();
            String refreshToken = socialToken.getRefreshToken();

            // 2. JWT 형식의 Client Secret 동적 생성
            String clientSecret = appleJwtUtil.createClientSecret();

            try {
                // 3. Apple Revoke API 호출을 위한 HTTP 연결 설정
                URL url = new URL(APPLE_REVOKE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // 4. 요청 본문(form-data) 구성
                String postData = String.format("client_id=%s&client_secret=%s&token=%s&token_type_hint=%s",
                        appleClientId, clientSecret, refreshToken, "refresh_token");

                // 5. 요청 전송
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 6. 응답 코드 확인
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // 200 OK가 아니면 실패로 간주하고 에러 메시지 반환
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        throw new AuthException("Apple Revoke API 호출 실패: " + response);
                    }
                }
                // 200 OK 응답을 받으면 성공으로 간주, 별도의 응답 본문은 없음
                // DB에서 해당 토큰 정보 삭제 로직은 AdminService에서 처리

            } catch (IOException e) {
                throw new AuthException("Apple Revoke API 호출 중 네트워크 오류 발생.", e);
            }
        } else {
            throw new AuthException("Apple Revoke API 호출 실패: 소셜 리프레시 토큰 없음");
        }
    }
}
