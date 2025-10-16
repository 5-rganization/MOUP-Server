package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "유저 FCM 토큰 갱신을 위한 DTO")
public class UserUpdateFCMTokenRequest {
  @Schema(description = "FCM 토큰", example = "4/0Ad-Q...very-long-string-of-code...", requiredMode = Schema.RequiredMode.REQUIRED)
  private String fcmToken;
}
