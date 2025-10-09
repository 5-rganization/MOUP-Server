package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * @author neoskyclad
 *
 * 회원가입을 위한 DTO
 */
@Getter
@Builder
@Schema(description = "회원가입을 위한 DTO")
public class RegisterRequest {
    @Schema(description = "닉네임", example = "moup1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
    @Schema(description = "유저 역할 (신규 가입 시 필요 - 알바생: ROLE_WORKER, 사장님: ROLE_OWNER)", example = "ROLE_WORKER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;
}
