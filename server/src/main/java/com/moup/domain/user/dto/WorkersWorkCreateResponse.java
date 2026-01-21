package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "근무자들에 대한 근무 생성 응답 DTO")
public class WorkersWorkCreateResponse {
    @Schema(description = "근무 생성에 성공한 근무자 ID 리스트 (없으면 빈 배열)", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> successWorkIdList;
    @Schema(description = "근무 생성에 실패힌 근무자 정보 리스트 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FailedWorkerInfo> failedWorkerInfoList;
}

