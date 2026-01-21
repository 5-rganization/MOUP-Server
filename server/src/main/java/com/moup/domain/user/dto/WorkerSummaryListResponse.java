package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "매장 근무자 요약 조회 응답 DTO")
public class WorkerSummaryListResponse {
    @Schema(description = "근무자 정보 배열", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkerSummaryResponse> workerSummaryInfoList;
}
