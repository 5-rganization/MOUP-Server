package com.moup.domain.auth.dto;

import com.moup.global.common.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회원가입 응답 DTO")
public class RegisterResponse {
    @Schema(description = "유저 역할", example = "ROLE_WORKER", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;
}
