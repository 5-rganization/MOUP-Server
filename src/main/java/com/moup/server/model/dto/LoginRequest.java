package com.moup.server.model.dto;

import com.moup.server.common.Login;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * @author neoskyclad
 *
 * 로그인 요청 DTO
 */
@Getter
@Builder
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    @Schema(description = "소셜 로그인 타입 (구글: LOGIN_GOOGLE, 애플: LOGIN_APPLE)", example = "LOGIN_GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Login provider;
    @Schema(description = "소셜 Auth Code", example = "4/0Ad-Q...very-long-string-of-code...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;
    @Schema(description = "유저 이름(신규 가입 시 필요 - Apple 한정)", example = "김모업", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String username;
}
