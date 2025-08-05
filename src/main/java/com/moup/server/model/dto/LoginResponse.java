package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {
    @Schema(description = "DB 유저 ID", example = "123456789")
    private String userId;
}
