package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알바생 홈 화면 근무지 요약 조회 응답 DTO")
public class WorkerHomeWorkplaceSummaryInfo {
    @Schema(description = "근무지 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkplaceSummaryResponse workplaceSummaryInfo;
    @Schema(description = "현재 근무 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isNowWorking;
}
