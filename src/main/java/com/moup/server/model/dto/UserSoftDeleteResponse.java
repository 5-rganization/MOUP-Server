package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 삭제 응답 DTO")
public class UserSoftDeleteResponse {
    @Schema(description = "서비스 유저 ID", example = "112233445566778899000")
    private String userId;
    @Schema(description = "삭제 처리 시각", example = "2025-01-01 12:34:56")
    private String deletedAt;
    @Schema(description = "삭제 요청 여부", example = "true")
    private Boolean isDeleted;
}
