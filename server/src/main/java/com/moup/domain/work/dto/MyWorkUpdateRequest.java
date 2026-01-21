package com.moup.domain.work.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moup.domain.work.domain.Work;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Getter
@Builder
@Schema(description = "사용자 근무 업데이트 요청 DTO")
public class MyWorkUpdateRequest {
    @NotNull(message = "값이 없을 경우 빈 배열을 전달해야 합니다.")
    @Schema(description = "연결할 루틴 ID 리스트 (없으면 빈 배열)", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> routineIdList;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "출근 시간 (ISO 8601 UTC)", example = "2025-10-11T08:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;
    @Schema(description = "실제 출근 시간 (ISO 8601 UTC)", example = "2025-10-11T08:35:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant actualStartTime;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "퇴근 시간 (ISO 8601 UTC)", example = "2025-10-11T15:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant endTime;
    @Schema(description = "실제 퇴근 시간 (ISO 8601 UTC)", example = "2025-10-11T15:40:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant actualEndTime;
    @NotNull(message = "값이 없을 경우 0을 전달해야 합니다.")
    @Schema(description = "휴게 시간 (분단위, 없을 경우 0)", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer restTimeMinutes;
    @Schema(description = "메모", example = "오늘 재고 정리하는 날", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String memo;
    @NotNull(message = "값이 없을 경우 빈 배열을 전달해야 합니다.")
    @Schema(description = "반복 요일 (없으면 빈 배열)", example = "[\"MONDAY\", \"WEDNESDAY\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DayOfWeek> repeatDays;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "반복 종료 날짜 (yyyy-MM-dd)", example = "2025-11-11", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate repeatEndDate;

    public Work toEntity(
            Long workId,
            Long workerId,
            Integer hourlyRate,
            int grossWorkMinutes,
            int netWorkMinutes,
            int nightWorkMinutes,
            int basePay,
            int nightAllowance,
            int holidayAllowance,
            String repeatGroupId
    ) {
        LocalDate workDate = startTime.atZone(SEOUL_ZONE_ID).toLocalDate();

        return Work.builder()
                .id(workId)
                .workerId(workerId)
                .workDate(workDate)
                .startTime(startTime.atZone(SEOUL_ZONE_ID).toLocalDateTime())
                .actualStartTime(actualStartTime != null ? actualStartTime.atZone(SEOUL_ZONE_ID).toLocalDateTime() : null)
                .endTime(endTime.atZone(SEOUL_ZONE_ID).toLocalDateTime())
                .actualEndTime(actualEndTime != null ? actualEndTime.atZone(SEOUL_ZONE_ID).toLocalDateTime() : null)
                .restTimeMinutes(restTimeMinutes)
                .grossWorkMinutes(grossWorkMinutes)
                .netWorkMinutes(netWorkMinutes)
                .nightWorkMinutes(nightWorkMinutes)
                .memo(memo)
                .hourlyRate(hourlyRate)
                .basePay(basePay)
                .nightAllowance(nightAllowance)
                .holidayAllowance(holidayAllowance)
                .grossIncome(basePay + nightAllowance + holidayAllowance)
                .estimatedNetIncome(0)
                .repeatGroupId(repeatGroupId)
                .build();
    }
}
