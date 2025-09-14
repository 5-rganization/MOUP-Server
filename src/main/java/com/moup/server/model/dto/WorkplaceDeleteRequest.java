package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 삭제 요청 DTO")
public class WorkplaceDeleteRequest {
    @Schema(description = "근무지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
}
