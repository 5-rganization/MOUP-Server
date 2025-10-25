package com.moup.server.model.entity;

import com.moup.server.model.enums.SalaryCalculation;
import com.moup.server.model.enums.SalaryType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.DayOfWeek;

@Getter
@Builder
@ToString
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
}
