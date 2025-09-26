package com.moup.server.model.dto;

import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "알바생 근무지 생성 요청 DTO")
public class WorkerWorkplaceCreateRequest extends BaseWorkplaceCreateRequest {
    @Schema(description = "라벨 색상 (알바생 기준)", example = "red", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;
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
    private Boolean hasNationalPension;
    @Schema(description = "건강보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasHealthInsurance;
    @Schema(description = "고용보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasEmploymentInsurance;
    @Schema(description = "산재보험 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIndustrialAccident;
    @Schema(description = "소득세 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasIncomeTax;
    @Schema(description = "야간수당 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasNightAllowance;

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

    public Worker toWorkerEntity(Long userId, Long workplaceId) {
        return Worker.builder()
                .id(null)
                .userId(userId)
                .workplaceId(workplaceId)
                .workerBasedLabelColor(workerBasedLabelColor)
                .isAccepted(true)
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
