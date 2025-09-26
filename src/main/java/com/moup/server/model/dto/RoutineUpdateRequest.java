package com.moup.server.model.dto;

import com.moup.server.exception.InvalidFieldFormatException;
import com.moup.server.model.entity.Routine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "루틴 이름", example = "오픈", requiredMode = Schema.RequiredMode.REQUIRED)
    private String routineName;
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "알람 시간은 HH:mm 형식(예: 14:30)으로 입력해주세요.")
    @Schema(description = "알림 시간 (HH:mm)", example = "14:30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alarmTime;
    @NotNull(message = "값이 없을 경우 빈 배열을 전달해야 합니다.")
    @Valid
    @Schema(description = "할 일 리스트",
            example = """
            [
                {
                    "taskId": 1,
                    "routineId": 1,
                    "content": "바닥 청소",
                    "orderIndex": 0,
                    "isChecked": true
                },
                {
                    "taskId": 2,
                    "routineId": 1,
                    "content": "전자레인지 청소",
                    "orderIndex": 1,
                    "isChecked": false
                }
            ]
            """,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoutineTaskUpdateRequest> routineTaskList;

    public Routine toEntity(Long userId) {
        if (alarmTime == null || alarmTime.isBlank()) {
            return Routine.builder()
                    .id(routineId)
                    .userId(userId)
                    .routineName(routineName)
                    .alarmTime(null)
                    .build();
        } else {
            try {
                return Routine.builder()
                        .id(routineId)
                        .userId(userId)
                        .routineName(routineName)
                        .alarmTime(LocalTime.parse(alarmTime, formatter))
                        .build();
            } catch (DateTimeParseException e) {
                throw new InvalidFieldFormatException();
            }
        }
    }
}
