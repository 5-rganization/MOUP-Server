package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "초대 코드 조회 응답 DTO")
public class InviteCodeInquiryResponse {
    @Schema(description = "조회된 근무지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "조회된 근무지 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "근무지 카테고리 이름", example = "편의점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;
}
