package com.moup.domain.routine.dto;

import com.moup.domain.workplace.dto.WorkplaceSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "오늘의 루틴 근무별 조회 응답 DTO")
public class TodayWorkRoutineCountResponse {
    @Schema(description = "근무 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workId;
    @Schema(description = "근무지(매장) 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkplaceSummaryResponse workplaceSummaryInfo;
    @Schema(description = "출근 시간 (ISO 8601 UTC)", example = "2025-10-11T08:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;
    @Schema(description = "퇴근 시간 (ISO 8601 UTC)", example = "2025-10-11T15:30:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant endTime;
    @Schema(description = "근무 시간 (분단위)", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workMinutes;
    @Schema(description = "근무에 연결된 루틴 개수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer routineCount;
}
