package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenCreateRequest {
    @Schema(description = "에러 코드", example = "ERROR_400")
    Long userId;
    @Schema(description = "에러 코드", example = "ERROR_400")
    String username;
}
