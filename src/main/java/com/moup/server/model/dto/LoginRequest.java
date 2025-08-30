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
    @Schema(description = "소셜 로그인 타입", example = "LOGIN_GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Login provider;
    @Schema(description = "소셜 인가 코드", example = "4/0Ad-Q...very-long-string-of-code...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;
//    @Schema(description = "코드 검증(구글 한정)")
//    private String codeVerifier;
}
