package com.moup.server.model.dto;

import com.moup.server.exception.InvalidDateTimeFormatException;
import com.moup.server.model.entity.Routine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 업데이트 요청 DTO")
public class RoutineUpdateRequest {
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "루틴 이름", example = "오픈", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @Schema(description = "알림 시간 (HH:mm)", example = "08:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alarmTime;
    @Schema(description = "할 일 리스트 (배열에 없는 할 일은 삭제)", example = "[ {\"할 일 ID\": 1, \"루틴 ID\": 1, \"내용\": \"바닥 청소\", \"정렬 순서\": 0, \"체크 여부\": false} ]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineTaskUpdateRequest> routineTaskUpdateRequestList;

    public Routine toEntity(Long userId) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return Routine.builder()
                    .id(routineId)
                    .userId(userId)
                    .routineName(routineName)
                    .alarmTime(LocalTime.parse(alarmTime, formatter))
                    .build();
        } catch (DateTimeParseException e) {
            throw new InvalidDateTimeFormatException();
        }
    }
}
