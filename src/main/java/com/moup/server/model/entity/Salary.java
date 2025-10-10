package com.moup.server.model.entity;

import lombok.*;

import java.time.DayOfWeek;

@Getter
@Builder
@ToString
public class Salary {
    private Long id;
    private Long workerId;
    private String salaryType;
    private String salaryCalculation;
    private Integer hourlyRate;
    private Integer fixedRate;
    private Integer salaryDate;
    private DayOfWeek salaryDay;
    private Boolean hasNationalPension;
    private Boolean hasHealthInsurance;
    private Boolean hasEmploymentInsurance;
    private Boolean hasIndustrialAccident;
    private Boolean hasIncomeTax;
    private Boolean hasNightAllowance;
}
