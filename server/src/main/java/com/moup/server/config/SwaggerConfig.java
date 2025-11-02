package com.moup.server.config;

import com.moup.server.model.dto.DebugTokenHolder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.info.Info;
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
    GroupedOpenApi authOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("인증 관련 API")
                .pathsToMatch("/auth/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi userOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("유저 관련 API")
                .pathsToMatch("/users/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi fileOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("파일 관련 API")
                .pathsToMatch("/files/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi adminOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("관리자 관련 API")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi workplaceOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("근무지 관련 API")
                .pathsToMatch("/workplaces/**")
                .pathsToExclude("/workplaces/**/workers/**", "/workplaces/**/works/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi workOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("근무 관련 API")
                .pathsToMatch("/**/works/**")
                .pathsToExclude("/**/routines/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi workerOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("근무자 관련 API")
                .pathsToMatch("/**/workers/**")
                .pathsToExclude("/**/works/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi routineOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("루틴 관련 API")
                .pathsToMatch("/routines/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi alarmOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("알림 관련 API")
                .pathsToMatch("/alarms/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }

    @Bean
    GroupedOpenApi homeOpenApi(OpenApiCustomizer debugTokenCustomizer) {
        return GroupedOpenApi.builder()
                .group("홈 화면 관련 API")
                .pathsToMatch("/home/**")
                .addOpenApiCustomizer(debugTokenCustomizer)
                .build();
    }


    // =================================================================
    // 디버그용 토큰 표시 메서드
    // =================================================================


    /// DebugTokenHolder를 Spring Bean으로 직접 등록합니다.
    /// `@Component` 어노테이션을 대체합니다.
    /// "prod" 프로필이 아닐 때만 Bean으로 등록해야 합니다.
    @Bean
    DebugTokenHolder debugTokenHolder() {
        // TODO: 배포시 보여주지 않도록 처리 (@Profile("!prod") 추가)
        return new DebugTokenHolder();
    }

    /// OpenAPI 명세서 전역 설정을 커스터마이징합니다.
    /// @param tokenHolder (debugTokenHolder Bean)
    /// "prod" 프로필이 아닐 때만 Bean으로 등록해야 합니다.
    @Bean
    OpenApiCustomizer debugTokenCustomizer(DebugTokenHolder tokenHolder) {
        // TODO: 배포시 보여주지 않도록 처리 (@Profile("!prod") 추가)
        return openApi -> {
            Info info = openApi.getInfo();
            String originalDescription = info.getDescription();

            String debugTokenDescription = String.format(
                    """
                    
                    ---
                    ### 🐞 디버그용 액세스 토큰 🐞
                    (개발 환경에서만 보이며, 서버 재시작 시마다 갱신됩니다)
                    
                    **Admin (관리자):**
                    `Bearer %s`
                    
                    **Owner (사장님):**
                    `Bearer %s`
                    
                    **Worker (알바생):**
                    `Bearer %s`
                    """,
                    tokenHolder.getAdminToken(),
                    tokenHolder.getOwnerToken(),
                    tokenHolder.getWorkerToken()
            );

            info.setDescription(originalDescription + debugTokenDescription);
        };
    }
}
