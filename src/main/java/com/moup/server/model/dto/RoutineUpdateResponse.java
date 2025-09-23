package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 업데이트 응답 DTO")
public class RoutineUpdateResponse {
    @Schema(description = "업데이트된 루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "업데이트된 할 일 ID 리스트 (없으면 빈 배열)", example = "[0, 1 ,2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> updatedTaskIdList;
    @Schema(description = "생성된 할 일 ID 리스트 (없으면 빈 배열)", example = "[3, 4]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> createdTaskIdList;
    @Schema(description = "삭제된 할 일 ID 리스트 (없으면 빈 배열)", example = "[5, 6]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> deletedTaskIdList;
}
