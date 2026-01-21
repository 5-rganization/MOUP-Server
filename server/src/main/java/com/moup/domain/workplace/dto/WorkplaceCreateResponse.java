package com.moup.domain.workplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 생성 응답 DTO")
public class WorkplaceCreateResponse {
    @Schema(description = "생성된 근무지(매장) ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
}
