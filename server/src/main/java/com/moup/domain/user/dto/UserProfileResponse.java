package com.moup.domain.user.dto;

import com.moup.global.common.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 프로필 응답 DTO")
public class UserProfileResponse {
    @Schema(description = "유저 ID", example = "1")
    private Long userId;
    @Schema(description = "유저 이름", example = "김모업")
    private String username;
    @Schema(description = "유저 닉네임", example = "moup1234")
    private String nickname;
    @Schema(description = "유저 프로필 사진 URL", example = "https://moup-bucket.s3.ap-northeast-2.amazonaws.com/5aedbc811d19b48d5151c9d05b48fc6751be282f5e89f478a3b81dbc16e2ada7.png")
    private String profileImg;
    @Schema(description = "유저 역할", example = "ROLE_WORKER")
    private Role role;
    @Schema(description = "생성 일자", example = "2025-01-01 12:34:56")
    private String createdAt;
}
