package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알바생 월간 근무 요약 조회 응답 DTO")
public class OwnerMonthlyWorkerSummaryResponse {
    @Schema(description = "사용자 닉네임", example = "김사장", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
    @Schema(description = "총 근무시간 (분)", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long totalWorkMinutes;
    @Schema(description = "세전 총소득 (원)", example = "70210", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer grossIncome;
    @Schema(description = "세후 실지급액 (원)", example = "70210", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer netIncome;
}
