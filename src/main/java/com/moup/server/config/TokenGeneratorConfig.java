package com.moup.server.config;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.model.dto.TokenCreateRequest;
import com.moup.server.model.entity.User;
import com.moup.server.util.JwtUtil;
import java.time.LocalDateTime;
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
      TokenCreateRequest testToken = TokenCreateRequest.builder()
          .role(Role.ROLE_ADMIN)
          .userId(1L)
          .username("김사장").build();

      // createToken 메서드를 사용하여 토큰 생성
      String staticToken = jwtUtil.createTestToken(testToken);

      // 토큰을 콘솔에 출력
      log.info("📢📢📢📢📢 Swagger Static Token Generated: {}", "Bearer " + staticToken);
    };
  }
}
