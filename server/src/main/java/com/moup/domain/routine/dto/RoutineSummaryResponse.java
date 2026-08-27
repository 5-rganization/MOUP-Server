package com.moup.domain.routine.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "루틴 요약 조회 응답 DTO")
public class RoutineSummaryResponse {

  @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long routineId;
  @Schema(description = "루틴 이름", example = "오픈 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
  private String routineName;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  @Schema(description = "알람 시간", example = "14:30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private LocalTime alarmTime;
  @Schema(description = "반복 요일 (없으면 빈 배열)", example = "[\"MONDAY\", \"WEDNESDAY\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private List<LinkedWorkRoutine> linkedWorks;
  /// 근무별 루틴 조회(`GET /routines/works/{workId}/routines`)에서만 채워진다.
  /// 그 화면이 체크박스를 그리려면 할 일과 완료 여부가 함께 필요한데, 루틴 상세 조회는
  /// 근무를 모르므로 완료 여부를 줄 수 없다. 근무 문맥이 없는 조회에서는 null이다.
  @Schema(description = "할 일 목록 (근무별 조회에서만 채워짐)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private List<RoutineTaskDetailResponse> routineTaskList;

  public record LinkedWorkRoutine(
      Long workId,
      List<DayOfWeek> repeatDays
  ) {

  }
}
