package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "급여 업데이트 응답 DTO")
public class WorkerSalaryUpdateResponse {
    @Schema(description = "업데이트된 급여의 근무자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
}
