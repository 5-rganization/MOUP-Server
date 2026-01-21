package com.moup.domain.workplace.dto;

import com.moup.domain.salary.dto.SalaryCreateRequest;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.user.domain.Worker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 참여 요청 DTO")
public class WorkplaceJoinRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{6}$", message = "초대 코드는 영문 또는 숫자로 이루어진 6자리여야 합니다.")
    @Schema(description = "참여할 근무지의 초대 코드", example = "MUP234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;
    @Valid
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "급여 생성 요청 DTO", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryCreateRequest salaryCreateRequest;

    public Worker toWorkerEntity(Long userId, Long workplaceId) {
        return Worker.builder()
                .userId(userId)
                .workplaceId(workplaceId)
                .workerBasedLabelColor(workerBasedLabelColor)
                .isAccepted(false)
                .isNowWorking(false)
                .build();
    }

    public Salary toSalaryEntity(Long workerId) {
        return Salary.builder()
                .workerId(workerId)
                .salaryType(salaryCreateRequest.getSalaryType())
                .salaryCalculation(salaryCreateRequest.getSalaryCalculation())
                .hourlyRate(salaryCreateRequest.getHourlyRate())
                .fixedRate(salaryCreateRequest.getFixedRate())
                .salaryDate(salaryCreateRequest.getSalaryDate())
                .salaryDay(salaryCreateRequest.getSalaryDay())
                .hasNationalPension(salaryCreateRequest.getHasNationalPension())
                .hasHealthInsurance(salaryCreateRequest.getHasHealthInsurance())
                .hasEmploymentInsurance(salaryCreateRequest.getHasEmploymentInsurance())
                .hasIndustrialAccident(salaryCreateRequest.getHasIndustrialAccident())
                .hasIncomeTax(salaryCreateRequest.getHasIncomeTax())
                .hasHolidayAllowance(salaryCreateRequest.getHasHolidayAllowance())
                .hasNightAllowance(salaryCreateRequest.getHasNightAllowance())
                .build();
    }
}
