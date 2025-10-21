package com.moup.server.config;

import com.moup.server.common.Role;
import com.moup.server.model.dto.TokenCreateRequest;
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
            // 테스트용 가상의 TokenCreate 객체 생성
            TokenCreateRequest adminToken = TokenCreateRequest.builder()
                    .role(Role.ROLE_ADMIN)
                    .userId(1L)
                    .username("관리자").build();
            TokenCreateRequest ownerToken = TokenCreateRequest.builder()
                    .role(Role.ROLE_OWNER)
                    .userId(2L)
                    .username("박사장").build();
            TokenCreateRequest workerToken = TokenCreateRequest.builder()
                    .role(Role.ROLE_WORKER)
                    .userId(3L)
                    .username("최알바").build();

            // createToken 메서드를 사용하여 토큰 생성
            String staticAdminToken = jwtUtil.createTestToken(adminToken);
            String staticOwnerToken = jwtUtil.createTestToken(ownerToken);
            String staticWorkerToken = jwtUtil.createTestToken(workerToken);

            // 토큰을 콘솔에 출력
            log.info("📢📢📢📢📢 Swagger Admin Token Generated: {}", "Bearer " + staticAdminToken);
            log.info("📢📢📢📢📢 Swagger Owner Token Generated: {}", "Bearer " + staticOwnerToken);
            log.info("📢📢📢📢📢 Swagger Worker Token Generated: {}", "Bearer " + staticWorkerToken);
        };
    }
}
