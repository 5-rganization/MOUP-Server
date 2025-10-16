package com.moup.server.model.dto;

import com.moup.server.model.entity.Salary;
import com.moup.server.model.enums.SalaryCalculation;
import com.moup.server.model.enums.SalaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;

@Getter
@Builder
@Schema(description = "알바생 급여 업데이트 요청 DTO")
public class SalaryUpdateRequest {
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "급여 유형 (매월: SALARY_MONTHLY, 매주: SALARY_WEEKLY, 매일: SALARY_DAILY)", example = "SALARY_MONTHLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryType salaryType;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "급여 계산 (시급: SALARY_CALCULATION_HOURLY, 고정: SALARY_CALCULATION_FIXED)", example = "SALARY_CALCULATION_HOURLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryCalculation salaryCalculation;
    @Positive
    @Schema(description = "시급", example = "10030", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer hourlyRate;
    @Positive
    @Schema(description = "고정급", example = "2156880", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer fixedRate;
    @Min(value = 1, message = "1부터 31까지의 숫자를 입력해야합니다.")
    @Max(value = 31, message = "1부터 31까지의 숫자를 입력해야합니다.")
    @Schema(description = "급여일", example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer salaryDate;
    @Schema(description = "급여 요일 (월: MONDAY, 화: TUESDAY, 수: WEDNESDAY, 목: THURSDAY, 금: FRIDAY, 토: SATURDAY, 일: SUNDAY)", example = "MONDAY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private DayOfWeek salaryDay;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "국민연금 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasNationalPension;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "건강보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasHealthInsurance;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "고용보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasEmploymentInsurance;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "산재보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIndustrialAccident;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "소득세 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIncomeTax;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "야간수당 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasNightAllowance;

    public Salary toEntity(Long salaryId, Long workerId) {
        return Salary.builder()
                .id(salaryId)
                .workerId(workerId)
                .salaryType(salaryType)
                .salaryCalculation(salaryCalculation)
                .hourlyRate(hourlyRate)
                .fixedRate(fixedRate)
                .salaryDate(salaryDate)
                .salaryDay(salaryDay)
                .hasNationalPension(hasNationalPension)
                .hasHealthInsurance(hasHealthInsurance)
                .hasEmploymentInsurance(hasEmploymentInsurance)
                .hasIndustrialAccident(hasIndustrialAccident)
                .hasIncomeTax(hasIncomeTax)
                .hasNightAllowance(hasNightAllowance)
                .build();
    }
}
