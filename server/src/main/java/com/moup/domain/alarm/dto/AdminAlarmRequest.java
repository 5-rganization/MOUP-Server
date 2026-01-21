package com.moup.domain.alarm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAlarmRequest {
  String title;
  String content;
}
