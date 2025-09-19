package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사장님 매장 업데이트 요청 DTO")
public class OwnerWorkplaceUpdateRequest implements WorkplaceUpdateRequest {
    @Schema(description = "매장 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "매장 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "매장 카테고리", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "라벨 색상", example = "red", requiredMode = Schema.RequiredMode.REQUIRED)
    private String labelColor;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;

    public Workplace toWorkplaceEntity(Long ownerId) {
        return Workplace.builder()
                .id(workplaceId)
                .ownerId(ownerId)
                .workplaceName(workplaceName)
                .categoryName(categoryName)
                .labelColor(labelColor)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
