package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 프로필 사진 응답 DTO")
public class UserProfileImageResponse {
    @Schema(description = "유저 ID", example = "1")
    private Long userId;
    @Schema(description = "유저 프로필 사진 URL", example = "https://moup-bucket.s3.ap-northeast-2.amazonaws.com/5aedbc811d19b48d5151c9d05b48fc6751be282f5e89f478a3b81dbc16e2ada7.png")
    private String imageUrl;
}
