package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "알바생 홈화면 응답 DTO")
public class WorkerHomeResponse extends BaseHomeResponse {
    private List<WorkerMonthlyWorkplaceSummaryResponse> workerMonthlyWorkplaceSummaryInfoList;
}
