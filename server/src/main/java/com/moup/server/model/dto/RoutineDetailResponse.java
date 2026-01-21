package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moup.server.model.dto.RoutineSummaryResponse.LinkedWorkRoutine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 상세 조회 응답 DTO")
public class RoutineDetailResponse {
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "루틴 이름", example = "오픈 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(description = "알람 시간 (HH:mm)", example = "14:30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalTime alarmTime;
    @Schema(description = "반복 요일 (없으면 빈 배열)", example = "[\"MONDAY\", \"WEDNESDAY\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<LinkedWorkRoutine> linkedWorks;
    @Schema(description = "할 일 리스트 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineTaskDetailResponse> routineTaskList;
}
