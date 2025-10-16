package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "근무 캘린더 조회 응답 DTO")
public class WorkCalendarListResponse {
    @Schema(description = "근무 요약 조회 배열 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkSummaryResponse> workSummaryList;
}