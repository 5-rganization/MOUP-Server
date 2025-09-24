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
public class OwnerWorkplaceUpdateRequest extends WorkplaceUpdateRequest {
    @Override
    @Schema(description = "매장 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long getWorkplaceId() { return super.getWorkplaceId(); }

    @Override
    @Schema(description = "매장 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getWorkplaceName() { return super.getWorkplaceName(); }

    @Override
    @Schema(description = "매장 카테고리 이름", example = "편의점", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getCategoryName() { return super.getCategoryName(); }

    @Override
    @Schema(description = "라벨 색상 (근무자 기준)", example = "red", requiredMode = Schema.RequiredMode.NOT_REQUIRED, hidden = true)
    public String getWorkerBasedLabelColor() { return super.getWorkerBasedLabelColor(); }

    @Override
    @Schema(description = "라벨 색상 (사장님 기준)", example = "red", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getOwnerBasedLabelColor() { return super.getOwnerBasedLabelColor(); }

    public Workplace toWorkplaceEntity(Long ownerId) {
        return Workplace.builder()
                .id(getWorkplaceId())
                .ownerId(ownerId)
                .workplaceName(getWorkplaceName())
                .categoryName(getCategoryName())
                .address(getAddress())
                .latitude(getLatitude())
                .longitude(getLongitude())
                .build();
    }
}
