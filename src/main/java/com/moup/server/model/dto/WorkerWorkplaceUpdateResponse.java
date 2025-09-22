package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알바생 근무지 업데이트 응답 DTO")
public class WorkerWorkplaceUpdateResponse {
    @Schema(description = "업데이트된 근무지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "근무지에 업데이트된 근무자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
}
