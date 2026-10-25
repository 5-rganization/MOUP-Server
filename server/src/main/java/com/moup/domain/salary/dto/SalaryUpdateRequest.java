package com.moup.domain.salary.dto;

import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryApplyFrom;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;

/// 급여 설정 **부분** 수정. 생략한 필드는 기존 값을 유지한다.
///
/// 예전에는 전 필드를 받아 통째로 덮어썼다. 그런데 근무지 상세 응답에 `fixedRate`·`salaryDay`가 없어
/// 클라이언트가 그 값을 되보낼 수 없었고, `salaryDay`를 모르는 앱이 수정할 때마다 NULL로 지워
/// **주급제 고정급의 월 급여가 0원**이 됐다(스코프 3 I-9). 사장님은 현재 설정을 조회할 수도 없이
/// 9개 필드를 짐작으로 채워 덮어써야 했다(스코프 3 I-8).
@Getter
@Builder
@Schema(description = "급여 업데이트 요청 DTO — 부분 수정. 생략한 필드는 기존 값을 유지한다")
public class SalaryUpdateRequest {
    @Schema(description = "급여 유형 (매월: SALARY_MONTHLY, 매주: SALARY_WEEKLY, 매일: SALARY_DAILY)", example = "SALARY_MONTHLY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private SalaryType salaryType;
    @Schema(description = "급여 계산 (시급: SALARY_CALCULATION_HOURLY, 고정: SALARY_CALCULATION_FIXED)", example = "SALARY_CALCULATION_HOURLY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    @Schema(description = "국민연금 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasNationalPension;
    @Schema(description = "건강보험 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasHealthInsurance;
    @Schema(description = "고용보험 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasEmploymentInsurance;
    @Schema(description = "산재보험 여부 — 표시 전용. 산재보험은 전액 사업주 부담이라 근로자 공제에 쓰지 않는다", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasIndustrialAccident;
    @Schema(description = "소득세 여부", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasIncomeTax;
    @Schema(description = "주휴수당 여부. 고정급은 주휴수당이 포함된 금액으로 보아 서버가 계산하지 않는다", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasHolidayAllowance;
    @Schema(description = "야간수당 여부", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasNightAllowance;
    @Schema(description = "연장근로 가산 여부 (1일 8시간·주 40시간 초과분 50%). 고정급은 계산하지 않는다", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasOvertimeAllowance;
    @Schema(description = "공휴일 근로 가산 여부 (8시간까지 50%, 초과분 100%). 고정급은 계산하지 않는다", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasPublicHolidayAllowance;
    @Schema(description = """
            변경을 어느 근무부터 적용할지 (오늘 근무부터: TODAY, 이번 달 1일 근무부터: THIS_MONTH). 생략하면 TODAY.
            그 이전 근무의 시급·수당은 바뀌지 않는다. 4대보험·세금은 월 단위라 이번 달 전체에 반영된다.""",
            example = "TODAY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private SalaryApplyFrom applyFrom;

    public SalaryApplyFrom resolveApplyFrom() {
        return applyFrom != null ? applyFrom : SalaryApplyFrom.TODAY;
    }

    /// 보낸 필드만 `current` 위에 덮어쓴 새 급여 설정을 만든다. `current`는 바꾸지 않는다.
    public Salary applyTo(Salary current) {
        return current.toBuilder()
                .salaryType(salaryType != null ? salaryType : current.getSalaryType())
                .salaryCalculation(salaryCalculation != null ? salaryCalculation : current.getSalaryCalculation())
                .hourlyRate(hourlyRate != null ? hourlyRate : current.getHourlyRate())
                .fixedRate(fixedRate != null ? fixedRate : current.getFixedRate())
                .salaryDate(salaryDate != null ? salaryDate : current.getSalaryDate())
                .salaryDay(salaryDay != null ? salaryDay : current.getSalaryDay())
                .hasNationalPension(hasNationalPension != null ? hasNationalPension : current.getHasNationalPension())
                .hasHealthInsurance(hasHealthInsurance != null ? hasHealthInsurance : current.getHasHealthInsurance())
                .hasEmploymentInsurance(hasEmploymentInsurance != null ? hasEmploymentInsurance : current.getHasEmploymentInsurance())
                .hasIndustrialAccident(hasIndustrialAccident != null ? hasIndustrialAccident : current.getHasIndustrialAccident())
                .hasIncomeTax(hasIncomeTax != null ? hasIncomeTax : current.getHasIncomeTax())
                .hasHolidayAllowance(hasHolidayAllowance != null ? hasHolidayAllowance : current.getHasHolidayAllowance())
                .hasNightAllowance(hasNightAllowance != null ? hasNightAllowance : current.getHasNightAllowance())
                .hasOvertimeAllowance(hasOvertimeAllowance != null ? hasOvertimeAllowance : current.getHasOvertimeAllowance())
                .hasPublicHolidayAllowance(hasPublicHolidayAllowance != null ? hasPublicHolidayAllowance : current.getHasPublicHolidayAllowance())
                .build();
    }
}
