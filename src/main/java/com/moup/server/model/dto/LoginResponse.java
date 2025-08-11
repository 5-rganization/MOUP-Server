package com.moup.server.model.dto;

import com.moup.server.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {
    @Schema(description = "서비스 유저 ID", example = "112233445566778899000")
    private String userId;
    @Schema(description = "유저 역할", example = "ROLE_WORKER")
    private Role role;
}
