package com.moup.server.model.dto;

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
  @Schema(description = "반복 요일 (없으면 빈 배열)", example = "[\"MONDAY\", \"WEDNESDAY\"]", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<LinkedWorkRoutine> linkedWorks;

  public record LinkedWorkRoutine(
      Long workId,
      List<DayOfWeek> repeatDays
  ) {

  }
}
