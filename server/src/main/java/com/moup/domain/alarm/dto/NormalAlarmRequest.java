package com.moup.domain.alarm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NormalAlarmRequest {
  Long senderId;
  Long receiverId;
  String title;
  String content;
}
