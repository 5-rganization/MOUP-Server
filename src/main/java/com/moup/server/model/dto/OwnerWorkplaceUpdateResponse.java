package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사장님 매장 업데이트 응답 DTO")
public class OwnerWorkplaceUpdateResponse {
    @Schema(description = "업데이트된 매장 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
}
