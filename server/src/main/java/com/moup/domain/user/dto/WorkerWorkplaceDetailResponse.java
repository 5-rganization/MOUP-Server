package com.moup.domain.user.dto;

import com.moup.domain.salary.dto.SalaryDetailResponse;
import com.moup.domain.workplace.dto.BaseWorkplaceDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "알바생 근무지 상세 조회 응답 DTO")
public class WorkerWorkplaceDetailResponse extends BaseWorkplaceDetailResponse {
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String workerBasedLabelColor;
    @Schema(description = "급여 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryDetailResponse salaryDetailInfo;
}
