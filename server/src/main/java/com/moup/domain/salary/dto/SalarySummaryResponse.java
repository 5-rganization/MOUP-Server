package com.moup.domain.salary.dto;

import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.domain.SalaryCalculation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;

@Getter
@Builder
@Schema(description = "급여 요약 조회 응답 DTO")
public class SalarySummaryResponse {
    @Schema(description = "급여 유형 (매월: SALARY_MONTHLY, 매주: SALARY_WEEKLY, 매일: SALARY_DAILY)", example = "SALARY_MONTHLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryType salaryType;
    @Schema(description = "급여 계산 (시급: SALARY_CALCULATION_HOURLY, 고정: SALARY_CALCULATION_FIXED)", example = "SALARY_CALCULATION_HOURLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryCalculation salaryCalculation;
    @Schema(description = "시급", example = "10030", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer hourlyRate;
    @Schema(description = "고정급", example = "2156880", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer fixedRate;
    @Schema(description = "급여일", example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer salaryDate;
    @Schema(description = "급여 요일 (월: MONDAY, 화: TUESDAY, 수: WEDNESDAY, 목: THURSDAY, 금: FRIDAY, 토: SATURDAY, 일: SUNDAY)", example = "MONDAY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private DayOfWeek salaryDay;
}
