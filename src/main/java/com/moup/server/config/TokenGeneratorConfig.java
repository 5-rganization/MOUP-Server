package com.moup.server.config;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.model.entity.User;
import com.moup.server.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TokenGeneratorConfig {
    private final JwtUtil jwtUtil;

    // 생성자 주입
    public TokenGeneratorConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public CommandLineRunner generateStaticToken() {
        return args -> {
            // 테스트용 가상의 User 객체 생성
            User testUser = User.builder()
                    .id(5L)
                    .provider(Login.LOGIN_GOOGLE)
                    .providerId("moup-admin")
                    .username("moup_admin")
                    .nickname("moup-admin")
                    .role(Role.ROLE_ADMIN)
                    .createdAt("2025-08-07 20:24:39")
                    .isDeleted(true)
                    .build();

            // createToken 메서드를 사용하여 토큰 생성
            String staticToken = jwtUtil.createAccessToken(testUser);

            // 토큰을 콘솔에 출력
            log.info("📢📢📢📢📢 Swagger Static Token Generated: {}", "Bearer " + staticToken);
        };
    }
}
