package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "사장님 매장 상세 조회 응답 DTO")
public class OwnerWorkplaceDetailResponse extends BaseWorkplaceDetailResponse {
    @Schema(description = "라벨 색상 (사장님 기준)", example = "BLUE", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String ownerBasedLabelColor;
}
