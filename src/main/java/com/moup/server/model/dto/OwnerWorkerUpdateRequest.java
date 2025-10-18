package com.moup.server.model.dto;

import com.moup.server.model.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Schema(description = "사장님 근무자 업데이트 요청 DTO")
public class OwnerWorkerUpdateRequest extends BaseWorkerUpdateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (사장님 기준)", example = "BLUE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerBasedLabelColor;

    public Worker toEntity(Long workerId, Long userId, Long workplaceId, String workerBasedLabelColor) {
        return Worker.builder()
                .id(workerId)
                .userId(userId)
                .workplaceId(workplaceId)
                .workerBasedLabelColor(workerBasedLabelColor)
                .ownerBasedLabelColor(ownerBasedLabelColor)
                .isAccepted(true)
                .build();
    }
}
