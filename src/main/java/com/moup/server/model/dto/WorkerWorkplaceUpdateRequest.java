package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "알바생 근무지 업데이트 요청 DTO")
public class WorkerWorkplaceUpdateRequest extends BaseWorkplaceUpdateRequest {
    @NotBlank(message = "빈 값이나 공백문자는 받을 수 없어요")
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;

    @Valid
    @Schema(description = "급여 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkerSalaryUpdateRequest salaryInfo;

    public Workplace toWorkplaceEntity(Long workplaceId, Long ownerId) {
        return Workplace.builder()
                .id(workplaceId)
                .ownerId(ownerId)
                .workplaceName(getWorkplaceName())
                .categoryName(getCategoryName())
                .address(getAddress())
                .latitude(getLatitude())
                .longitude(getLongitude())
                .build();
    }
}
