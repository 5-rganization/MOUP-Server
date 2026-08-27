package com.moup.domain.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 용 알림 요청 DTO")
public class AdminNotificationRequest {
  @NotNull(message = "필수 입력값입니다.")
  @Positive(message = "1 이상의 값만 입력해야 합니다.")
  @Schema(description = "수신자 ID", example = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  Long receiverId;
  @NotBlank(message = "필수 입력값입니다.")
  @Schema(description = "제목", example = "초대 코드 전송", requiredMode = Schema.RequiredMode.REQUIRED)
  String title;
  @NotBlank(message = "필수 입력값입니다.")
  @Schema(description = "내용", example = "ABCDEFG", requiredMode = Schema.RequiredMode.REQUIRED)
  String content;
}
