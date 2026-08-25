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

    /// 토큰 용도 구분 클레임. 이 값이 없으면 refresh token을 access token으로 쓸 수 있다.
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

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
                .claim(TOKEN_TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(TokenCreateRequest tokenCreateRequest) {
        return Jwts.builder()
                .subject(String.valueOf(tokenCreateRequest.getUserId()))
                .claim(TOKEN_TYPE_CLAIM, TYPE_REFRESH)
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

    /// 토큰의 용도(`typ`)를 반환한다. 클레임이 없으면 null.
    public String getTokenType(String token) {
        return (String) Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(TOKEN_TYPE_CLAIM);
    }

    /// 서명·만료가 유효하면서 용도가 access인지 확인한다.
    public boolean isValidAccessToken(String token) {
        return isValidToken(token) && TYPE_ACCESS.equals(getTokenType(token));
    }

    /// 서명·만료가 유효하면서 용도가 refresh인지 확인한다.
    public boolean isValidRefreshTokenType(String token) {
        return isValidToken(token) && TYPE_REFRESH.equals(getTokenType(token));
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
