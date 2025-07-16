package com.moup.moup_server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author neoskyclad
 *
 * 로그인 요청 DTO
 */
@Getter
@Setter
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    @Schema(description = "소셜 로그인 타입", example = "LOGIN_GOOGLE")
    private String provider;
    @Schema(description = "소셜 로그인 ID 또는 토큰", example = "123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com")
    private String providerId;
}
