package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알바생 월간 근무지 요약 조회 응답 DTO")
public class WorkerMonthlyWorkplaceSummaryResponse {
    @Schema(description = "근무지 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkerHomeWorkplaceSummaryInfo homeWorkplaceSummaryInfo;
    @Schema(description = "월급날까지 남은 날", example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer daysUntilPayday;
    @Schema(description = "총 근무시간 (분)", example = "420", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long totalWorkMinutes;
    @Schema(description = "총 주간시간 (분)", example = "360", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dayTimeMinutes;
    @Schema(description = "총 야간시간 (분)", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long nightTimeMinutes;
    @Schema(description = "총 휴게시간 (분)", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long restTimeMinutes;
    @Schema(description = "총 주휴수당 (원)", example = "10000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer totalHolidayAllowance;
    @Schema(description = "총 야간수당 (원)", example = "15000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer totalNightAllowance;
    @Schema(description = "세전 총소득 (원)", example = "70210", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer grossIncome;
    @Schema(description = "국민연금 (원)", example = "3160", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer nationalPension;
    @Schema(description = "건강보험 (원)", example = "2810", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer healthInsurance;
    @Schema(description = "고용보험 (원)", example = "630", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer employmentInsurance;
    @Schema(description = "소득세 (원)", example = "0", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer incomeTax;
    @Schema(description = "세후 실지급액 (원)", example = "63610", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer netIncome;
}
