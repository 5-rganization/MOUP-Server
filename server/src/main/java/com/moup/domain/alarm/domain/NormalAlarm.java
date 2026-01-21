package com.moup.domain.alarm.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NormalAlarm {
  Long id;
  Long senderId;
  Long receiverId;
  String title;
  String content;
  LocalDateTime sentAt;
  LocalDateTime readAt;
}
