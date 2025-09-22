package com.moup.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "MOUP Server", description = "MOUP API 명세서 입니다.", version = "v1.0.5"))
public class SwaggerConfig {

    @Bean
    GroupedOpenApi authOpenApi() {
        String[] paths = {"/auth/**"};
        return GroupedOpenApi.builder().group("인증 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi userOpenApi() {
        String[] paths = {"/users/**"};
        return GroupedOpenApi.builder().group("유저 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi fileOpenApi() {
        String[] paths = {"/files/**"};
        return GroupedOpenApi.builder().group("파일 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi adminOpenApi() {
        String[] paths = {"/admin/**"};
        return GroupedOpenApi.builder().group("관리자 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi workplaceOpenApi() {
        String[] paths = {"/workplace/**"};
        return GroupedOpenApi.builder().group("근무지 관련 API").pathsToMatch(paths).build();
    }
}
