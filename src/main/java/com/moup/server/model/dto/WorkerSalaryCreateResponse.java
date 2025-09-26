package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "급여 생성 응답 DTO")
public class WorkerSalaryCreateResponse {
    @Schema(description = "생성된 급여의 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salaryId;
}
