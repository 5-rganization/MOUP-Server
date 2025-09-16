package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지 중복 확인 응답 DTO")
public class WorkplaceDuplicateCheckResponse {
    @Schema(description = "중복 확인 결과", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isDuplicated;
}
