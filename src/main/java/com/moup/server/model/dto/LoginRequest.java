package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author neoskyclad
 *
 * 로그인 요청 DTO
 */
@Getter
@Builder
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    @Schema(description = "소셜 로그인 타입", example = "LOGIN_GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String provider;
    @Schema(description = "소셜 로그인 ID 또는 토큰", example = "123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String providerId;
}
