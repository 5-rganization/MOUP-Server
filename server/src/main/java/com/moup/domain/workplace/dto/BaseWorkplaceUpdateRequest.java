package com.moup.domain.workplace.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.moup.domain.user.dto.OwnerWorkplaceUpdateRequest;
import com.moup.domain.user.dto.WorkerWorkplaceUpdateRequest;
import com.moup.domain.workplace.domain.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(OwnerWorkplaceUpdateRequest.class), @JsonSubTypes.Type(WorkerWorkplaceUpdateRequest.class)})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseWorkplaceUpdateRequest {
    @Schema(description = "근무지(매장) 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "근무지(매장) 카테고리 이름", example = "편의점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;
    public abstract Workplace toWorkplaceEntity(Long workplaceId, Long ownerId);
}
