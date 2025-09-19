package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 생성 응답 DTO")
public class RoutineCreateResponse {
    @Schema(description = "생성된 루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "생성된 할 일 ID 리스트", example = "[0, 1 ,2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> taskIdList;
}
