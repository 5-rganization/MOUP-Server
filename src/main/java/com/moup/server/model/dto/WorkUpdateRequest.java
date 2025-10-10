package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moup.server.model.entity.Work;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "근무 업데이트 요청 DTO")
public class WorkUpdateRequest {
    @NotNull(message = "값이 없을 경우 빈 배열을 전달해야 합니다.")
    @Schema(description = "연결할 루틴 ID 리스트 (없으면 빈 배열)", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> routineIdList;
    @NotNull(message = "필수 입력값입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "실제 출근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 08:35", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime actualStartTime;
    @NotNull(message = "필수 입력값입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "실제 퇴근 시간 (yyyy-MM-dd HH:mm)", example = "2025-10-11 15:40", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime actualEndTime;
    @NotNull(message = "값이 없을 경우 0을 전달해야 합니다.")
    @Schema(description = "휴게 시간 (분단위, 없을 경우 0)", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer restTime;
    @Schema(description = "메모", example = "단체 회의 있음", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String memo;
    @Schema(description = "반복 요일", example = "[MONDAY, WEDNESDAY]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<DayOfWeek> repeatDays;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "반복 종료 날짜 (yyyy-MM-dd)", example = "2025-11-11", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate repeatEndDate;

    public Work toEntity(Long workId, Long workerId, LocalDate workDate, Integer hourlyRate, Integer dailyIncome) {
        return Work.builder()
                .id(workId)
                .workerId(workerId)
                .workDate(workDate)
                .startTime(startTime)
                .actualStartTime(actualStartTime)
                .endTime(endTime)
                .actualEndTime(actualEndTime)
                .restTime(restTime)
                .memo(memo)
                .hourlyRate(hourlyRate)
                .dailyIncome(dailyIncome)
                .repeatDays(repeatDays)
                .repeatEndDate(repeatEndDate)
                .build();
    }
}
