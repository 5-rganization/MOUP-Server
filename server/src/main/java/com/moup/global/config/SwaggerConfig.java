package com.moup.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "MOUP Server",
        description = """
            MOUP API 명세서 입니다.
            
            액세스 토큰을 HTTP 요청 헤더의 Authorization 필드에 담아 보내주세요.
            - 형식) Bearer라는 단어 뒤에 한 칸을 띄고 발급받은 토큰 문자열을 붙임
            - 예시) "Authorization": "Bearer eyJhbGciOiJIUzI1"
            
            Swagger에서 테스트 시 우측 Authorize 버튼을 누르고 발급받은 액세스 토큰을 넣어주시면 됩니다.
            """,
        version = "v1.0.5"
    ),
    servers = {
        @Server(url = "/", description = "Default Server URL")
    }
)
public class SwaggerConfig {

  @Bean
  public GroupedOpenApi authOpenApi() {
    return GroupedOpenApi.builder()
        .group("인증 관련 API")
        .pathsToMatch("/auth/**")
        .build();
  }

  @Bean
  public GroupedOpenApi userOpenApi() {
    return GroupedOpenApi.builder()
        .group("유저 관련 API")
        .pathsToMatch("/users/**")
        .build();
  }

  @Bean
  public GroupedOpenApi fileOpenApi() {
    return GroupedOpenApi.builder()
        .group("파일 관련 API")
        .pathsToMatch("/files/**")
        .build();
  }

  @Bean
  public GroupedOpenApi adminOpenApi() {
    return GroupedOpenApi.builder()
        .group("관리자 관련 API")
        .pathsToMatch("/admin/**")
        .build();
  }

  @Bean
  public GroupedOpenApi workplaceOpenApi() {
    return GroupedOpenApi.builder()
        .group("근무지 관련 API")
        .pathsToMatch("/workplaces/**")
        .pathsToExclude("/workplaces/**/workers/**", "/workplaces/**/works/**")
        .build();
  }

  @Bean
  public GroupedOpenApi workOpenApi() {
    return GroupedOpenApi.builder()
        .group("근무 관련 API")
        .pathsToMatch("/**/works/**")
        .pathsToExclude("/**/routines/**")
        .build();
  }

  @Bean
  public GroupedOpenApi workerOpenApi() {
    return GroupedOpenApi.builder()
        .group("근무자 관련 API")
        .pathsToMatch("/**/workers/**")
        .pathsToExclude("/**/works/**")
        .build();
  }

  @Bean
  public GroupedOpenApi routineOpenApi() {
    return GroupedOpenApi.builder()
        .group("루틴 관련 API")
        .pathsToMatch("/routines/**")
        .build();
  }

  @Bean
  public GroupedOpenApi alarmOpenApi() {
    return GroupedOpenApi.builder()
        .group("알림 관련 API")
        .pathsToMatch("/alarms/**")
        .build();
  }

  @Bean
  public GroupedOpenApi homeOpenApi() {
    return GroupedOpenApi.builder()
        .group("홈 화면 관련 API")
        .pathsToMatch("/home/**")
        .build();
  }
}
