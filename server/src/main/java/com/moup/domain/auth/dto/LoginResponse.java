package com.moup.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moup.global.common.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "유저 역할", example = "ROLE_WORKER", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Role role;
    @Schema(description = "액세스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
    @Schema(description = "리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
