package com.moup.server.model.dto;

import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알바생 근무지 생성 요청 DTO")
public class WorkerWorkplaceCreateRequest {
    @Schema(description = "근무지 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "근무지 카테고리 이름", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "급여 유형", example = "매월: SALARY_MONTHLY, 매주: SALARY_WEEKLY, 매일: SALARY_DAILY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String salaryType;
    @Schema(description = "급여 계산", example = "시급: SALARY_CALCULATION_HOURLY, 고정: SALARY_CALCULATION_FIXED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String salaryCalculation;
    @Schema(description = "시급", example = "10030", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer hourlyRate;
    @Schema(description = "고정급", example = "2156880", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer fixedRate;
    @Schema(description = "급여일", example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer salaryDate;
    @Schema(description = "급여 요일", example = "월: MON, 화: TUE, 수: WED, 목: THU, 금: FRI, 토: SAT, 일: SUN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String salaryDay;
    @Schema(description = "국민연금 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNationalPension;
    @Schema(description = "건강보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasHealthInsurance;
    @Schema(description = "고용보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasEmploymentInsurance;
    @Schema(description = "산재보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasIndustrialAccident;
    @Schema(description = "소득세 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasIncomeTax;
    @Schema(description = "야간수당 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNightAllowance;
    @Schema(description = "라벨 색상 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String labelColor;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;

    public Workplace toWorkplaceEntity(Long ownerId) {
        return Workplace.builder()
                .id(null)
                .ownerId(ownerId)
                .workplaceName(workplaceName)
                .categoryName(categoryName)
                .labelColor(labelColor)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public Salary toSalaryEntity(Long workerId) {
        return Salary.builder()
                .id(workerId)
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
