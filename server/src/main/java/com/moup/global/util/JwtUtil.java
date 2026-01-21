package com.moup.global.util;

import com.moup.global.security.token.TokenCreateRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.access.token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration;

    private final Key key;

    public JwtUtil(@Value("${jwt.secret.key}") String secretKey) {
        log.debug(secretKey);
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(TokenCreateRequest tokenCreateRequest) {
        return Jwts.builder()
                .subject(String.valueOf(tokenCreateRequest.getUserId()))
                .claim("role", tokenCreateRequest.getRole().name())
                .claim("username", tokenCreateRequest.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(TokenCreateRequest tokenCreateRequest) {
        return Jwts.builder()
                .subject(String.valueOf(tokenCreateRequest.getUserId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String createTestToken(TokenCreateRequest tokenCreateRequest) {
        long oneYearInMilliseconds = 1000L * 60 * 60 * 24 * 365; // 1년 (밀리초)

        return Jwts.builder()
                .subject(String.valueOf(tokenCreateRequest.getUserId()))
                .claim("role", tokenCreateRequest.getRole().name())
                .claim("username", tokenCreateRequest.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + oneYearInMilliseconds))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    public String getUsername(String token) {
        return (String) Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username");
    }

    public String getUserRole(String token) {
        return (String) Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public boolean isValidToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT Token: {}", e.getMessage());
            return false;
        }
    }
}
