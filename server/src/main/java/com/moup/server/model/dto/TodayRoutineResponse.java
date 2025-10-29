package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "오늘의 루틴 조회 응답 DTO")
public class TodayRoutineResponse {
    @Schema(description = "오늘 루틴이 있는 근무 리스트 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TodayWorkRoutineCountResponse> todayWorkRoutineCountList;
}
