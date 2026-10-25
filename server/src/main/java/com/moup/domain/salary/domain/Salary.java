package com.moup.domain.salary.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.DayOfWeek;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Salary {
    private Long id;
    private Long workerId;
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;
    @Enumerated(EnumType.STRING)
    private SalaryCalculation salaryCalculation;
    private Integer hourlyRate;
    private Integer fixedRate;
    private Integer salaryDate;
    @Enumerated(EnumType.STRING)
    private DayOfWeek salaryDay;
    private Boolean hasNationalPension;
    private Boolean hasHealthInsurance;
    private Boolean hasEmploymentInsurance;
    private Boolean hasIndustrialAccident;
    private Boolean hasIncomeTax;
    private Boolean hasHolidayAllowance;
    private Boolean hasNightAllowance;
    /// 연장근로 가산(50%) 적용 여부 (Phase 11 D11-2). 5인 이상 사업장만 의무라 기본은 꺼짐이다.
    private Boolean hasOvertimeAllowance;
    /// 공휴일 근로 가산(8시간까지 50%, 넘으면 100%) 적용 여부 (Phase 11 Q6). 기본은 꺼짐이다.
    private Boolean hasPublicHolidayAllowance;

    /// 스키마의 조합 제약(`ck_salaries_rate`)과 같다 — 시급제는 시급이, 고정급제는 고정급이 있어야 한다.
    /// 부분 수정은 요청만 봐서는 알 수 없으므로 **기존 값과 합친 결과**로 검사한다.
    public boolean isRateConsistentWithCalculation() {
        if (salaryCalculation == null) {
            return false;
        }
        return switch (salaryCalculation) {
            case SALARY_CALCULATION_HOURLY -> hourlyRate != null;
            case SALARY_CALCULATION_FIXED -> fixedRate != null;
        };
    }
}
