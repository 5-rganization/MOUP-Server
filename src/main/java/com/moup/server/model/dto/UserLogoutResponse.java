package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그아웃 응답 DTO")
public class UserLogoutResponse {
  @Schema(description = "유저 ID", example = "1")
  Long userId;
}
