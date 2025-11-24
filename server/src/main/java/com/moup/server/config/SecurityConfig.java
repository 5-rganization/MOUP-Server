package com.moup.server.config;

import com.moup.server.filter.JwtFilter;
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
  private final String[] USER_AUTH_URL = {"/users/**", "/files/**", "/workplaces/**",
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
          corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
          corsConfig.setAllowedHeaders(List.of("*"));
          corsConfig.setAllowCredentials(true);
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
