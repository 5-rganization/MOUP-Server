package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "액세스 토큰 재발급 응답 DTO")
public class RefreshTokenResponse {
    @Schema(description = "액세스 토큰")
    String accessToken;
    @Schema(description = "리프레시 토큰")
    String refreshToken;
}
