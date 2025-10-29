package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "알바생 출석 정보 응답 DTO")
public class WorkerWorkAttendanceResponse {
    @Schema(description = "근무 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "근무 날짜 (yyyy-MM-dd)", example = "2025-10-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate workDate;
    @Schema(description = "출근 시간 (ISO 8601 UTC)", example = "2025-10-11T08:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;
    @Schema(description = "실제 출근 시간 (ISO 8601 UTC)", example = "2025-10-11T08:35:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant actualStartTime;
    @Schema(description = "퇴근 시간 (ISO 8601 UTC)", example = "2025-10-11T15:30:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant endTime;
    @Schema(description = "실제 퇴근 시간 (ISO 8601 UTC)", example = "2025-10-11T15:40:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant actualEndTime;
}
