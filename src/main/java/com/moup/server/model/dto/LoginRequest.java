package com.moup.server.model.dto;

import com.moup.server.common.Login;
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
    @Schema(description = "소셜 로그인 타입 (구글: LOGIN_GOOGLE, 애플: LOGIN_APPLE)", example = "LOGIN_GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Login provider;
    @Schema(description = "소셜 인가 코드", example = "4/0Ad-Q...very-long-string-of-code...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;
//    @Schema(description = "코드 검증(구글 한정)")
//    private String codeVerifier;
    @Schema(description = "유저 이름(Apple 한정)", example = "김모업", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    @Schema(description = "닉네임", example = "moup1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
    @Schema(description = "유저 역할 (알바생: ROLE_WORKER, 사장님: ROLE_OWNER)", example = "ROLE_WORKER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;
}
