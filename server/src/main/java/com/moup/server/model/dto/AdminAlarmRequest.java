package com.moup.server.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAlarmRequest {
  String title;
  String content;
}
