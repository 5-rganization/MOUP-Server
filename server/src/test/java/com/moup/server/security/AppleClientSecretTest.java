package com.moup.server.security;

import com.moup.global.util.AppleJwtUtil;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Apple client secret의 헤더가 deprecated API를 걷어낸 뒤에도 같은지 (스코프 1 M9).
class AppleClientSecretTest {

    @Test
    @DisplayName("client secret 헤더에 kid와 alg=ES256이 있고, 공개키로 서명이 검증된다")
    void 헤더와_서명() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        String secret = new AppleJwtUtil("TEAM123", "KEY456", pem, "com.moup.app").createClientSecret();

        var jws = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(secret);
        assertEquals("KEY456", jws.getHeader().getKeyId());
        assertEquals("ES256", jws.getHeader().getAlgorithm());
        assertEquals("TEAM123", jws.getPayload().getIssuer());
    }
}
