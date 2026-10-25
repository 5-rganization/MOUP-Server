package com.moup.domain.salary.dto;

import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;

@Getter
@Builder
@Schema(description = "급여 상세 조회 응답 DTO")
public class SalaryDetailResponse {
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
    @Schema(description = "국민연금 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasNationalPension;
    @Schema(description = "건강보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasHealthInsurance;
    @Schema(description = "고용보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasEmploymentInsurance;
    @Schema(description = "산재보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIndustrialAccident;
    @Schema(description = "소득세 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIncomeTax;
    @Schema(description = "주휴수당 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasHolidayAllowance;
    @Schema(description = "야간수당 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasNightAllowance;
    @Schema(description = "연장근로 가산 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasOvertimeAllowance;

    /// 저장된 급여 설정 전체. 수정 폼이 이 응답을 채워 되보내므로 **필드를 빠뜨리면 안 된다** —
    /// 예전 조립 코드는 `fixedRate`·`salaryDay`를 빠뜨려 고정급 금액이 0으로 채워졌다(스코프 3 I-9).
    public static SalaryDetailResponse from(Salary salary) {
        return SalaryDetailResponse.builder()
                .salaryType(salary.getSalaryType())
                .salaryCalculation(salary.getSalaryCalculation())
                .hourlyRate(salary.getHourlyRate())
                .fixedRate(salary.getFixedRate())
                .salaryDate(salary.getSalaryDate())
                .salaryDay(salary.getSalaryDay())
                .hasNationalPension(salary.getHasNationalPension())
                .hasHealthInsurance(salary.getHasHealthInsurance())
                .hasEmploymentInsurance(salary.getHasEmploymentInsurance())
                .hasIndustrialAccident(salary.getHasIndustrialAccident())
                .hasIncomeTax(salary.getHasIncomeTax())
                .hasHolidayAllowance(salary.getHasHolidayAllowance())
                .hasNightAllowance(salary.getHasNightAllowance())
                .hasOvertimeAllowance(Boolean.TRUE.equals(salary.getHasOvertimeAllowance()))
                .build();
    }
}
