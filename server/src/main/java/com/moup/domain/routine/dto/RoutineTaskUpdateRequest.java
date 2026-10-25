package com.moup.domain.routine.dto;

import com.moup.domain.routine.domain.RoutineTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "할 일 업데이트 요청 DTO")
public class RoutineTaskUpdateRequest {
    /// 기존 할 일을 고치는 것이면 그 ID. 없으면 새 할 일이다. 예전에는 ID를 받지 않아 서버가 할 일을 전부 지우고
    /// 다시 만들었고, 그 할 일들의 근무별 체크(`routine_task_completions`)가 CASCADE로 함께 사라졌다.
    @Schema(description = "기존 할 일 ID (새 할 일이면 생략). 보내야 그 할 일의 체크 기록이 유지된다", example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long taskId;
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Size(max = 30, message = "30자 이내로 입력해야 합니다.")
    @Schema(description = "내용", example = "바닥 청소", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderIndex;

    public RoutineTask toEntity(Long routineId) {
        return RoutineTask.builder()
                .id(null)
                .routineId(routineId)
                .content(content)
                .orderIndex(orderIndex)
                .build();
    }
}
