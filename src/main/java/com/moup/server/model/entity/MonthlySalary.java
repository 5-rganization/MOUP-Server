package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.YearMonth;

@Getter
@Builder
@ToString
public class MonthlySalary {
    private Long id;
    private Long workerId;
    private YearMonth salaryMonth;  // 정산 연월 (e.g., 2025-10)
    private Integer grossIncome;  // 세전 총소득
    private Integer nationalPension;  // 국민연금 공제액
    private Integer healthInsurance;  // 건강보험 공제액
    private Integer employmentInsurance;  // 고용보험 공제액
    private Integer incomeTax;  // 소득세 공제액
    private Integer localIncomeTax;  // 지방소득세 공제액
    private Integer netIncome;  // 세후 실지급액
}
