package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 삭제 철회 응답 DTO")
public class UserRestoreResponse {
    @Schema(description = "유저 ID", example = "1")
    private Long userId;
    @Schema(description = "삭제 처리 시각")
    private String deletedAt;
    @Schema(description = "삭제 요청 여부", example = "false")
    private boolean isDeleted;
}
