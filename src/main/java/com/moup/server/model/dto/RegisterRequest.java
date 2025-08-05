package com.moup.server.model.dto;

import com.moup.server.common.Login;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author neoskyclad
 *
 * 회원가입을 위한 DTO
 */
@Getter
@Setter
@Schema(description = "회원가입을 위한 DTO")
public class RegisterRequest {
    @Schema(description = "소셜 로그인 타입", example = "LOGIN_GOOGLE")
    private Login provider;
    @Schema(description = "소셜 로그인 ID 또는 토큰", example = "123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com")
    private String idToken;
    @Schema(description = "닉네임", example = "neoskyclad")
    private String nickname;
    @Schema(description = "유저 역할", example = "ROLE_WORKER")
    private String role;
}
