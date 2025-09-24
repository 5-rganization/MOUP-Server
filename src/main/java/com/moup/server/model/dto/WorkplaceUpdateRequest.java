package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "근무지(매장) 업데이트 요청의 공통 필드를 담는 추상 DTO")
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class WorkplaceUpdateRequest {
    private Long workplaceId;
    private String workplaceName;
    private String categoryName;
    private String workerBasedLabelColor;
    private String ownerBasedLabelColor;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;
    public abstract Workplace toWorkplaceEntity(Long ownerId);
}
