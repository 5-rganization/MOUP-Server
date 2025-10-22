package com.moup.server.model.dto;

import com.moup.server.model.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "알바생 근무자 업데이트 요청 DTO")
public class WorkerWorkerUpdateRequest extends BaseWorkerUpdateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerBasedLabelColor;

    public Worker toEntity(Long workerId, Long userId, Long workplaceId, String ownerBasedLabelColor) {
        return Worker.builder()
                .id(workerId)
                .userId(userId)
                .workplaceId(workplaceId)
                .workerBasedLabelColor(workerBasedLabelColor)
                .ownerBasedLabelColor(ownerBasedLabelColor)
                .isAccepted(true)
                .isNowWorking(false)
                .build();
    }
}
