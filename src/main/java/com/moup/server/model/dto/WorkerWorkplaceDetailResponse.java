package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "알바생 근무지 상세 조회 응답 DTO")
public class WorkerWorkplaceDetailResponse extends BaseWorkplaceDetailResponse {
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;
    @Schema(description = "급여 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryDetailResponse salaryDetailInfo;
}
