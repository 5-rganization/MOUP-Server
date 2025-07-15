package com.moup.moup_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String[] NO_AUTH_URL = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/swagger-ui.html",
    "/auth/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors
                        .configurationSource(request -> {
                            var corsConfig = new CorsConfiguration();
                            corsConfig.setAllowedOrigins(List.of("*"));  // TODO: 프론트엔드 도메인으로 바꾸기
                            corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                            corsConfig.setAllowedHeaders(List.of("*"));
                            corsConfig.setAllowCredentials(true);
                            return corsConfig;
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(NO_AUTH_URL)
                        .permitAll()    // TODO: 서비스 때 접속 막아놓기
//                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                );
        return http.build();
    }
}
