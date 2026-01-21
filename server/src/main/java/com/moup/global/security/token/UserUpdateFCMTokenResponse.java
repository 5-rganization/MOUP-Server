package com.moup.global.security.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 FCM 토큰 갱신 응답 DTO")
public class UserUpdateFCMTokenResponse {
  @Schema(description = "유저 ID", example = "1")
  Long userId;
}
