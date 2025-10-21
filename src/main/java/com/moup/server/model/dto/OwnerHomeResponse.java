package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "사장님 홈화면 응답 DTO")
public class OwnerHomeResponse extends BaseHomeResponse {
    @Schema(description = "사장님 월간 매장 요약 정보 배열 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OwnerMonthlyWorkplaceSummaryResponse> ownerMonthlyWorkplaceSummaryInfoList;
}
