package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "오늘의 루틴 근무별 조회 응답 DTO")
public class TodayWorkRoutineCountResponse {
    @Schema(description = "근무 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workId;
    @Schema(description = "근무지(매장) 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkplaceSummaryResponse workplaceSummaryInfo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
    @Schema(description = "근무 시간 (분단위)", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workMinutes;
    @Schema(description = "근무에 연결된 루틴 개수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer routineCount;
}
