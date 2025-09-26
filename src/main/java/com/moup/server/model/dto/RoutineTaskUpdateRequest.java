package com.moup.server.model.dto;

import com.moup.server.model.entity.RoutineTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "할 일 업데이트 요청 DTO")
public class RoutineTaskUpdateRequest {
    @Schema(description = "할 일 ID", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long taskId;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "내용", example = "바닥 청소", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderIndex;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "체크 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isChecked;

    public RoutineTask toEntity() {
        return RoutineTask.builder()
                .id(taskId)
                .routineId(routineId)
                .content(content)
                .orderIndex(orderIndex)
                .isChecked(isChecked)
                .build();
    }
}
