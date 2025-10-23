package com.moup.server.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmContent {
  // 포맷 템플릿으로 정의
  ALARM_CONTENT_WORKPLACE_JOIN_REQUEST("%s님이 근무지 참가 요청을 보냈습니다.");

  private final String content;

  public String getContent(Object... args) {
    return String.format(this.content, args);
  }
}
