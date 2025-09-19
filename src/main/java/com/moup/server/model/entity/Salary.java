package com.moup.server.model.entity;

import lombok.*;

@Getter
@Builder
@ToString
public class Salary {
    private Long id;
    private String salaryType;
    private String salaryCalculation;
    private Integer hourlyRate;
    private Integer fixedRate;
    private Integer salaryDate;
    private String salaryDay;
    private boolean hasNationalPension;
    private boolean hasHealthInsurance;
    private boolean hasEmploymentInsurance;
    private boolean hasIndustrialAccident;
    private boolean hasIncomeTax;
    private boolean hasNightAllowance;
}
