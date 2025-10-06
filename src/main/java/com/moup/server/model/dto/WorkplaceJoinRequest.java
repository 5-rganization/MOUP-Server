package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 참여 요청 DTO")
public class WorkplaceJoinRequest {
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "참여할 근무지 ID", example = "MUP234", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;
    @NotNull(message = "급여 정보는 필수 입력값입니다.")
    @Schema(description = "알바생 급여 생성 요청 DTO", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkerSalaryCreateRequest salaryInfo;
}
