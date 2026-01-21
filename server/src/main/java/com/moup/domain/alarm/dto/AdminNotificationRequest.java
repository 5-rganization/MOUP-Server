package com.moup.domain.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 용 알림 요청 DTO")
public class AdminNotificationRequest {
  @Schema(description = "수신자 ID", example = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  Long receiverId;
  @Schema(description = "제목", example = "초대 코드 전송", requiredMode = Schema.RequiredMode.REQUIRED)
  String title;
  @Schema(description = "내용", example = "ABCDEFG", requiredMode = Schema.RequiredMode.REQUIRED)
  String content;
}
