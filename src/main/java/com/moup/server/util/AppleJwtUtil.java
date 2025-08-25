package com.moup.server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.KeyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class AppleJwtUtil {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final String JWT_ALGORITHM = "ES256";

    private final String appleTeamId;
    private final String appleKeyId;
    private final PrivateKey privateKey;
    private final String appleClientId;

    public AppleJwtUtil(@Value("${apple.team.id}") String appleTeamId, @Value("${apple.key.id}") String appleKeyId,
                        @Value("${apple.private.key}") String applePrivateKey,
                        @Value("${apple.client.id}") String appleClientId) {
        this.appleTeamId = appleTeamId;
        this.appleKeyId = appleKeyId;
        this.privateKey = loadPrivateKey(applePrivateKey);
        this.appleClientId = appleClientId;
    }

    private PrivateKey loadPrivateKey(String privateKeyContent) {
        try {
            // PKCS8 PEM 형식의 개인키를 파싱합니다.
            String pem = privateKeyContent.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

            // KeyFactory를 사용하여 EC(Elliptic Curve) 개인키를 생성합니다.
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("애플 개인키를 로드하는 중 오류가 발생했습니다.", e);
        }
    }

    public String createClientSecret() {
        // JWT의 만료 시간은 5분(300초)으로 설정합니다.
        // Apple 정책에 따라 최대 6개월까지 유효합니다.
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + TimeUnit.MINUTES.toMillis(5));

        return Jwts.builder()
                // 헤더(Header)
                .setHeaderParam("kid", appleKeyId).setHeaderParam("alg", JWT_ALGORITHM)
                // 페이로드(Payload)
                .claim("iss", appleTeamId)                 // Issuer: 팀 ID
                .claim("iat", issuedAt.getTime() / 1000)   // Issued At: 발급 시간 (Unix 타임)
                .claim("exp", expiration.getTime() / 1000) // Expiration: 만료 시간 (Unix 타임)
                .claim("aud", APPLE_AUDIENCE)              // Audience: https://appleid.apple.com
                .claim("sub", appleClientId)           // Subject: 우리 앱의 Client ID
                .signWith(privateKey, Jwts.SIG.ES256)      // 개인키로 ES256 알고리즘을 사용해 서명
                .compact();
    }
}
