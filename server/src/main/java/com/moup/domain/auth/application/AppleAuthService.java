package com.moup.domain.auth.application;

import com.google.gson.Gson;
import com.moup.domain.auth.domain.Login;
import com.moup.global.security.token.SocialTokenRepository;
import com.moup.global.util.AppleJwtUtil;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppleAuthService extends BaseAuthService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER_URL = "https://appleid.apple.com";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    @Value("${apple.client.id}")
    private String appleClientId;
    @Value("${apple.redirect.uri}")
    private String appleRedirectUri;

    private final JWKSource<SecurityContext> jwkSet;
    private final AppleJwtUtil appleJwtUtil;

    public AppleAuthService(@Value("${apple.team.id}") String appleTeamId,
                            @Value("${apple.key.id}") String appleKeyId,
                            @Value("${apple.private.key}") String applePrivateKey,
                            @Value("${apple.client.id}") String appleClientId,
                            SocialTokenRepository socialTokenRepository) throws MalformedURLException {
        super(socialTokenRepository);
        this.appleJwtUtil = new AppleJwtUtil(appleTeamId, appleKeyId, applePrivateKey, appleClientId);
        this.jwkSet = JWKSourceBuilder.create(new URL(APPLE_JWKS_URL)).build();
    }

    @Override
    public Login getProvider() { return Login.LOGIN_APPLE; }

    @Override
    protected String getRevokeUrl() { return APPLE_REVOKE_URL; }

    @Override
    protected String buildRevokeRequestBody(String refreshToken) {
        String clientSecret = appleJwtUtil.createClientSecret();
        return String.format("client_id=%s&client_secret=%s&token=%s&token_type_hint=%s",
                appleClientId, clientSecret, refreshToken, "refresh_token");
    }

    @Override
    public Map<String, Object> exchangeAuthCode(String authCode) throws AuthException {
        try {
            // 1. JWT 형식의 Client Secret 동적 생성
            String clientSecret = appleJwtUtil.createClientSecret();

            // 2. 토큰 요청을 위한 본문(body) 데이터 구성
            String postData = String.format("client_id=%s&client_secret=%s&code=%s&grant_type=%s&redirect_uri=%s",
                    appleClientId, clientSecret, authCode, "authorization_code", appleRedirectUri);

            // 3. 공통 헬퍼를 사용해 Apple 서버에 토큰 요청 후, 응답(JSON)을 문자열로 받기
            URL tokenUrl = new URL(APPLE_TOKEN_URL);
            String responseBody = sendPostRequestAndGetResponse(tokenUrl, postData);

            // 4. JSON 응답 파싱 및 토큰 추출
            Gson gson = new Gson();
            Map<String, Object> tokenResponse = gson.fromJson(responseBody, Map.class);
            String idTokenString = (String) tokenResponse.get("id_token");
            String socialAccessToken = (String) tokenResponse.get("access_token");
            String socialRefreshToken = (String) tokenResponse.get("refresh_token");

            // 5. ID 토큰 검증
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSet);
            jwtProcessor.setJWSKeySelector(keySelector);

            // ID 토큰의 Audience, Issuer 등 필수 클레임 검증 설정
            jwtProcessor.setJWTClaimsSetVerifier(
                    new DefaultJWTClaimsVerifier<>(
                            new HashSet<>(Collections.singletonList(appleClientId)), // Audience
                            new JWTClaimsSet.Builder().issuer(APPLE_ISSUER_URL).build(), // Issuer
                            new HashSet<>(Arrays.asList("exp", "iat", "sub")), // 필수 클레임
                            null
                    )
            );

            JWTClaimsSet claimsSet = jwtProcessor.process(idTokenString, null);

            // 6. 검증된 토큰에서 사용자 정보 추출
            String userId = claimsSet.getSubject();  // 사용자의 고유 ID
            String email = claimsSet.getStringClaim("email");

            // 7. 결과 정보를 Map에 담아 반환
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("email", email);  // Apple 정책에 따라 최초 1회만 제공될 수 있음
            userInfo.put("socialAccessToken", socialAccessToken);
            userInfo.put("socialRefreshToken", socialRefreshToken);

            return userInfo;

        } catch (JOSEException | ParseException | BadJOSEException | IOException e) {
            throw new AuthException("Apple 인증 코드 교환 또는 ID 토큰 검증에 실패했습니다.", e);
        }
    }
}
