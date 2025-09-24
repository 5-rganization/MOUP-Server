package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "모든 루틴 요약 조회 응답 DTO")
public class RoutineSummaryListResponse {
    @Schema(description = "루틴 요약 조회 배열 (없으면 빈 배열)", example = "[ {\"routineId\": 1, \"routineName\": \"오픈 루틴\", \"알람 시간\": \"08:00\"} ]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineSummaryResponse> routineSummaryList;
}
