package com.moup.global.security.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "로그아웃 요청 DTO")
public class UserLogoutRequest {
  @Schema(description = "로그아웃하는 기기의 FCM 토큰. 생략하면 이 유저의 모든 기기에서 푸시를 끊는다.",
      example = "4/0Ad-Q...very-long-string-of-code...",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String fcmToken;
}
