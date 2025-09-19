package com.moup.server.model.dto;

import com.moup.server.model.entity.RoutineTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "할 일 생성 요청 DTO")
public class RoutineTaskCreateRequest {
    @Schema(description = "내용", example = "바닥 청소", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    @Schema(description = "정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderIndex;
    @Schema(description = "체크 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isChecked;

    public RoutineTask toEntity(Long routineId) {
        return RoutineTask.builder()
                .id(null)
                .routineId(routineId)
                .content(content)
                .orderIndex(orderIndex)
                .isChecked(isChecked)
                .build();
    }
}
