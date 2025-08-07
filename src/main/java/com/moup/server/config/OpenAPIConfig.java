package com.moup.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenAPIConfig {

    // TODO: 디벨롭 이후에 지우기
    private static final String STATIC_TOKEN = "Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI1Iiwicm9sZSI6IlJPTEVfV09SS0VSIiwidXNlcm5hbWUiOiJtb3VwXzEiLCJpYXQiOjE3NTQ1NjYwMTcsImV4cCI6MTc1NDU2NzIxN30.Xcspp3Bon3FbFdvihteL5OCMV4px-Lz2PPAimalaQggC1ojSRrULNkV_6TgK4yPT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MOUP Server API")
                        .version("v1.0.5")
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer-Auth"))
                .components(new Components()
                        .addSecuritySchemes("Bearer-Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Bearer token for authentication")
                        )
                );
    }
}