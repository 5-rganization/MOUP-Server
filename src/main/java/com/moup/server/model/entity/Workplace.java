package com.moup.server.model.entity;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Workplace {
    private Long id;
    private Long owner_id;
    private String workplaceName;
    private String categoryName;
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
    private String labelColor;
    private String address;
    private Double latitude;
    private Double longitude;
}
