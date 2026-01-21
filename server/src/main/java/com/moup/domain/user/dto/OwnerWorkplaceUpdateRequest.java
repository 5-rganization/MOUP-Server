package com.moup.domain.user.dto;

import com.moup.domain.workplace.dto.BaseWorkplaceUpdateRequest;
import com.moup.domain.workplace.domain.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "사장님 매장 업데이트 요청 DTO")
public class OwnerWorkplaceUpdateRequest extends BaseWorkplaceUpdateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Schema(description = "라벨 색상 (사장님 기준)", example = "BLUE", requiredMode = Schema.RequiredMode.REQUIRED)
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
