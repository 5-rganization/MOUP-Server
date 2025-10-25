package com.moup.server.common;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AlarmTitle {
  ALARM_TITLE_WORKPLACE_JOIN_REQUEST("근무지 참가 요청"),
  ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED("근무지 참가 승인");

  private final String title;
}
