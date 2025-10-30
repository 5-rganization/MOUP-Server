package com.moup.server.model.dto;

import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "알바생 근무지 생성 요청 DTO")
public class WorkerWorkplaceCreateRequest extends BaseWorkplaceCreateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;

    @Valid
    @Schema(description = "급여 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryCreateRequest salaryCreateRequest;

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
                .isNowWorking(false)
                .build();
    }
}
