package com.moup.domain.routine.dto;

import com.moup.domain.routine.domain.RoutineTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "할 일 생성 요청 DTO")
public class RoutineTaskCreateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없습니다.")
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
