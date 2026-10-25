package com.moup.global.config;

import com.moup.global.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final String[] NO_AUTH_URL = {"/auth/**"};
  private final String[] HEALTH_CHECK_URL = {"/health"};
  // /works/**와 /home/**가 빠져 있어 anyRequest().authenticated()로 떨어졌다 — 역할 검사 없이 인증만 봤다(스코프 1 M8).
  private final String[] USER_AUTH_URL = {"/users/**", "/files/**", "/workplaces/**", "/works/**", "/home/**",
      "/routines/**", "/alarms/**"};
  private final String[] ADMIN_AUTH_URL = {"/admin/**"};
  private final String[] SWAGGER_URL = {"/v3/api-docs/**", "/swagger-ui/**",
      "/swagger-resources/**",
      "/swagger-ui.html",
      "/"};

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(request -> {
          var corsConfig = new CorsConfiguration();
          corsConfig.setAllowedOriginPatterns(List.of("*"));
          // PATCH가 빠져 있었다. 스프링 시큐리티의 CORS가 WebMvc 쪽보다 먼저 동작하므로
          // WebConfig에 PATCH가 있어도 preflight가 여기서 막혔다.
          corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
          corsConfig.setAllowedHeaders(List.of("*"));
          // 인증은 Authorization 헤더(Bearer)로만 한다. 와일드카드 origin에 credentials까지 열면
          // 쿠키 인증을 도입하는 순간 어느 사이트에서든 자격 증명이 실린 요청을 보낼 수 있다(스코프 1 M2).
          corsConfig.setAllowCredentials(false);
          return corsConfig;
        })).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .formLogin(AbstractHttpConfigurer::disable).authorizeHttpRequests(
            auth -> auth.requestMatchers(NO_AUTH_URL).permitAll()
                .requestMatchers(HEALTH_CHECK_URL).permitAll()
                .requestMatchers(USER_AUTH_URL).hasAnyRole("WORKER", "OWNER", "ADMIN")
                .requestMatchers(SWAGGER_URL).permitAll()    // TODO: 나중에 swagger 비활성화 하기
                .requestMatchers(ADMIN_AUTH_URL).hasRole("ADMIN").anyRequest().authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
          res.setStatus(401);
        }).accessDeniedHandler((req, res, e) -> {
          res.setStatus(403);
        }));
    return http.build();
  }
}
