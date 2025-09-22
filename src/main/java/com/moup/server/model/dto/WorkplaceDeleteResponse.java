package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지(매장) 삭제 응답 DTO")
public class WorkplaceDeleteResponse {
    @Schema(description = "삭제된 근무지(매장) ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
}
