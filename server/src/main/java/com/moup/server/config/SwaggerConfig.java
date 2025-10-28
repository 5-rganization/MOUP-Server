package com.moup.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "MOUP Server",
        description = """
        MOUP API 명세서 입니다.
        
        액세스 토큰을 HTTP 요청 헤더의 Authorization 필드에 담아 보내주세요.
        - 형식) Bearer라는 단어 뒤에 한 칸을 띄고 발급받은 토큰 문자열을 붙임
        - 예시) "Authorization": "Bearer eyJhbGciOiJIUzI1"
        
        Swagger에서 테스트 시 우측 Authorize 버튼을 누르고 발급받은 액세스 토큰을 넣어주시면 됩니다.
        """,
        version = "v1.0.5"))
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
        String[] pathsToMatch = {"/workplaces/**"};
        String[] pathsToExclude = {"/workplaces/**/workers/**", "/workplaces/**/works/**"};
        return GroupedOpenApi.builder().group("근무지 관련 API").pathsToMatch(pathsToMatch).pathsToExclude(pathsToExclude).build();
    }

    @Bean
    GroupedOpenApi workOpenApi() {
        String[] pathsToMatch = {"/**/works/**"};
        return GroupedOpenApi.builder().group("근무 관련 API").pathsToMatch(pathsToMatch).build();
    }

    @Bean
    GroupedOpenApi workerOpenApi() {
        String[] paths = {"/**/workers/**"};
        String[] pathsToExclude = {"/**/works/**"};
        return GroupedOpenApi.builder().group("근무자 관련 API").pathsToMatch(paths).pathsToExclude(pathsToExclude).build();
    }

    @Bean
    GroupedOpenApi routineOpenApi() {
        String[] paths = {"/routines/**"};
        return GroupedOpenApi.builder().group("루틴 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi alarmOpenApi() {
        String[] paths = {"/alarms/**"};
        return GroupedOpenApi.builder().group("알림 관련 API").pathsToMatch(paths).build();
    }

    @Bean
    GroupedOpenApi homeOpenApi() {
        String[] paths = {"/home/**"};
        return GroupedOpenApi.builder().group("홈 화면 관련 API").pathsToMatch(paths).build();
    }
}
