package com.moup.server.model.dto;

import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "사장님 매장 생성 요청 DTO")
public class OwnerWorkplaceCreateRequest extends BaseWorkplaceCreateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (사장님 기준)", example = "BLUE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerBasedLabelColor;

    public Workplace toWorkplaceEntity(Long userId) {
        return Workplace.builder()
                .id(null)
                .ownerId(userId)
                .workplaceName(getWorkplaceName())
                .categoryName(getCategoryName())
                .isShared(true)
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
                .ownerBasedLabelColor(ownerBasedLabelColor)
                .isAccepted(true)
                .build();
    }
}
