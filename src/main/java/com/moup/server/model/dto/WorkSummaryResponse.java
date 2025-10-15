package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "근무 요약 조회 응답 DTO")
public class WorkSummaryResponse {
    @Schema(description = "근무지(매장) 요약 정보",
            example =
                    """
                    {
                        "workplaceId": 1,
                        "workplaceName": "세븐일레븐 동탄중심상가점",
                        "isShared": true
                    }
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkplaceSummaryResponse workplaceSummary;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "근무 날짜 (yyyy-MM-dd)", example = "2025-10-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate workDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
    @Schema(description = "근무 시간 (분단위)", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workMinutes;
    @Schema(description = "휴게 시간 (분단위, 없을 경우 0)", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer restTimeMinutes;
    @Schema(description = "반복 요일", example = "[MONDAY, WEDNESDAY]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<DayOfWeek> repeatDays;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "반복 종료 날짜 (yyyy-MM-dd)", example = "2025-11-11", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate repeatEndDate;
}
