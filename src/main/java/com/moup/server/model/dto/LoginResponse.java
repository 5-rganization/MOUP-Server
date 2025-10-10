package com.moup.server.model.dto;

import com.moup.server.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {
    @Schema(description = "유저 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
    @Schema(description = "유저 역할", example = "ROLE_WORKER", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Role role;
    @Schema(description = "액세스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
    @Schema(description = "리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
