package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moup.server.model.entity.Routine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 생성 요청 DTO")
public class RoutineCreateRequest {
    @NotBlank(message = "빈 값이나 공백 문자는 받을 수 없어요")
    @Schema(description = "루틴 이름", example = "오픈 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(description = "알림 시간 (HH:mm)", example = "14:30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalTime alarmTime;
    @NotNull(message = "값이 없을 경우 빈 배열을 전달해야 합니다.")
    @Valid
    @Schema(description = "할 일 리스트 (없으면 빈 배열)",
            example = """
            [
                {
                    "content": "바닥 청소",
                    "orderIndex": 0
                },
                {
                    "content": "전자레인지 청소",
                    "orderIndex": 1
                }
            ]
            """,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineTaskCreateRequest> routineTaskList;

    public Routine toEntity(Long userId) {
        return Routine.builder()
                .id(null)
                .userId(userId)
                .routineName(routineName)
                .alarmTime(alarmTime)
                .build();
    }
}
