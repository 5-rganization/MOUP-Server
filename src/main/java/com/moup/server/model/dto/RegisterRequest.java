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
@Schema(description = "회원가입을 위한 DTO")
public class RegisterRequest {
    @Schema(description = "소셜 로그인 타입", example = "LOGIN_GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Login provider;
    @Schema(description = "소셜 인가 코드", example = "4/0Ad-Q...very-long-string-of-code...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;
//    @Schema(description = "코드 검증(구글 한정)")
//    private String codeVerifier;
    @Schema(description = "유저 이름(Apple 한정)", example = "김모업")
    private String username;
    @Schema(description = "닉네임", example = "moup1234")
    private String nickname;
    @Schema(description = "유저 역할", example = "ROLE_WORKER")
    private String role;
}
