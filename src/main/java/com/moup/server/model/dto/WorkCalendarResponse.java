package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "근무 캘린더 조회 응답 DTO")
public class WorkCalendarResponse {

    private List<WorkSummaryResponse> workSummaryList;
}
