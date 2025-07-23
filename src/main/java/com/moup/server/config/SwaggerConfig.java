package com.moup.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "MOUP Server", description = "MOUP API 명세서 입니다.", version = "v1.0.2"))
public class SwaggerConfig {

    @Bean
    GroupedOpenApi authOpenApi() {
        String[] paths = {"/auth/**"};
        return GroupedOpenApi.builder().group("Auth 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi userOpenApi() {
        String[] paths = {"/user/**"};
        return GroupedOpenApi.builder().group("유저 관련 API").pathsToMatch(paths).build();
    }
}
