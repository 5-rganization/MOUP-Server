package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "루틴 요약 조회 응답 DTO")
public class RoutineSummaryResponse {
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "루틴 이름", example = "오픈 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @Schema(description = "알람 시간", example = "14:30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alarmTime;
}
