package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "알바생 출석 정보 응답 DTO")
public class WorkerWorkAttendanceResponse {
    @Schema(description = "근무 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "근무 날짜 (yyyy-MM-dd)", example = "2025-10-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate workDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "실제 출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:35", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime actualStartTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "실제 퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:40", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime actualEndTime;
}
