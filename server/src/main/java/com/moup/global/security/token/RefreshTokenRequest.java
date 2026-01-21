package com.moup.global.security.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "액세스 토큰 재발급을 위한 DTO")
public class RefreshTokenRequest {
    @Schema(description = "리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
