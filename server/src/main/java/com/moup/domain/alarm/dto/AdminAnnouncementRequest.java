package com.moup.domain.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공지 요청 DTO")
public class AdminAnnouncementRequest {
  @NotBlank(message = "필수 입력값입니다.")
  @Schema(description = "제목", example = "전체 공지", requiredMode = Schema.RequiredMode.REQUIRED)
  String title;
  @NotBlank(message = "필수 입력값입니다.")
  @Schema(description = "내용", example = "전체 공지 드립니다.", requiredMode = Schema.RequiredMode.REQUIRED)
  String content;
}
