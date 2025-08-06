package com.moup.server.service;

import com.moup.server.common.Login;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

    private static final String APPLE_ISSUER_URL = "https://appleid.apple.com";
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    @Value("${apple.client.id}")
    private String appleClientId;

    private final JWKSource<SecurityContext> jwkSet;

    public AppleAuthService() throws MalformedURLException {
        this.jwkSet = JWKSourceBuilder.create(new URL(APPLE_JWKS_URL)).build();
    }

    @Override
    public Login getProvider() {
        return Login.LOGIN_APPLE;
    }

    @Override
    public Map<String, Object> verifyIdToken(String idTokenString)
            throws GeneralSecurityException, ParseException {
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        // 1. 서명 알고리즘 설정
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSet);
        jwtProcessor.setJWSKeySelector(keySelector);

        // 2. JWT 클레임 검증기(Verifier) 설정
        JWTClaimsSet exactMatchClaims = new JWTClaimsSet.Builder()
                .issuer(APPLE_ISSUER_URL)
                .build();
        Set<String> requiredClaims = new HashSet<>();
        requiredClaims.add("exp");  // 만료 시간
        requiredClaims.add("iat");  // 발급 시간

        jwtProcessor.setJWTClaimsSetVerifier(
                new DefaultJWTClaimsVerifier<>(
                        new HashSet<>(Collections.singletonList(appleClientId)),
                        exactMatchClaims,
                        requiredClaims,
                        null
                )
        );

        JWTClaimsSet claimsSet;
        try {
            // 3. 토큰 처리 및 검증 (서명, 클레임 검증 포함)
            claimsSet = jwtProcessor.process(idTokenString, null);
        } catch (BadJOSEException | ParseException | JOSEException e) {
            throw new GeneralSecurityException("Invalid Apple ID token: " + e.getMessage(), e);
        }

        // 4. 클레임에서 사용자 정보 추출 및 Map에 담기
        String userId = claimsSet.getSubject();
        String email = claimsSet.getStringClaim("email");
        Boolean emailVerified = claimsSet.getBooleanClaim("email_verified");

        Map<String, Object> userInfo = new HashMap<>();
        if (userId != null) {
            userInfo.put("userId", userId);
        }
        if (email != null) {
            userInfo.put("email", email);
        }
        if (emailVerified != null) {
            userInfo.put("emailVerified", emailVerified ? "true" : "false");
        }

        return userInfo;
    }
}
