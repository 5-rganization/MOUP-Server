package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 생성 요청 DTO")
public class RoutineCreateRequest {
    @Schema(description = "루틴 이름", example = "오전 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @Schema(description = "알림 시간 (HH:mm)", example = "08:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alarmTime;
    @Schema(description = "할 일 리스트", example = "\"내용\": \"바닥 청소\", \"정렬 순서\": 0, \"체크 여부\": false", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineTaskCreateRequest> routineTaskCreateRequestList;
}
