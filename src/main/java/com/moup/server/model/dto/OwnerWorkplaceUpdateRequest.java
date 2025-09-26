package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "사장님 매장 업데이트 요청 DTO")
public class OwnerWorkplaceUpdateRequest extends BaseWorkplaceUpdateRequest {
    @Schema(description = "라벨 색상 (사장님 기준)", example = "blue", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerBasedLabelColor;

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
