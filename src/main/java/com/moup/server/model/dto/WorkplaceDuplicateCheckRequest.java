package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 중복 확인 요청 DTO")
public class WorkplaceDuplicateCheckRequest {
    @Schema(description = "등록자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ownerId;
    @Schema(description = "근무지 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
}
