package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지(매장) 요약 조회 응답 DTO")
public class WorkplaceSummaryResponse {
    @Schema(description = "근무지(매장) ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "근무지(매장) 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "공유 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isShared;
    @Schema(description = "근무자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
}
