package com.moup.server.model.dto;

import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "알바생 근무지 생성 요청 DTO")
public class WorkerWorkplaceCreateRequest extends WorkplaceCreateRequest {
    @Override
    @Schema(description = "근무지 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getWorkplaceName() { return super.getWorkplaceName(); }

    @Override
    @Schema(description = "근무지 카테고리 이름", example = "편의점", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getCategoryName() { return super.getCategoryName(); }

    @Schema(description = "급여 유형 (매월: SALARY_MONTHLY, 매주: SALARY_WEEKLY, 매일: SALARY_DAILY)", example = "SALARY_MONTHLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String salaryType;
    @Schema(description = "급여 계산 (시급: SALARY_CALCULATION_HOURLY, 고정: SALARY_CALCULATION_FIXED)", example = "SALARY_CALCULATION_HOURLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String salaryCalculation;
    @Schema(description = "시급", example = "10030", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer hourlyRate;
    @Schema(description = "고정급", example = "2156880", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer fixedRate;
    @Schema(description = "급여일", example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer salaryDate;
    @Schema(description = "급여 요일 (월: MON, 화: TUE, 수: WED, 목: THU, 금: FRI, 토: SAT, 일: SUN)", example = "MON", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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

    @Override
    @Schema(description = "라벨 색상 (근무자 기준)", example = "red", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getWorkerBasedLabelColor() { return super.getWorkerBasedLabelColor(); }

    @Override
    @Schema(description = "라벨 색상 (사장님 기준)", example = "red", requiredMode = Schema.RequiredMode.NOT_REQUIRED, hidden = true)
    public String getOwnerBasedLabelColor() { return super.getOwnerBasedLabelColor(); }

    public Workplace toWorkplaceEntity(Long userId) {
        return Workplace.builder()
                .id(null)
                .ownerId(userId)
                .workplaceName(getWorkplaceName())
                .categoryName(getCategoryName())
                .isShared(false)
                .address(getAddress())
                .latitude(getLatitude())
                .longitude(getLongitude())
                .build();
    }

    public Salary toSalaryEntity(Long workerId) {
        return Salary.builder()
                .id(null)
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
