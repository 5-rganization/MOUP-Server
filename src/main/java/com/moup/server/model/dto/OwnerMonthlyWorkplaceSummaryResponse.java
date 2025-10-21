package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "사장님 월간 매장 요약 조회 응답 DTO")
public class OwnerMonthlyWorkplaceSummaryResponse {
    @Schema(description = "매장 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkplaceSummaryResponse workplaceSummaryInfo;
    @Schema(description = "근무자 월간 근무 요약 조회 정보 배열", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OwnerMonthlyWorkerSummaryResponse> monthlyWorkerSummaryInfoList;
}
