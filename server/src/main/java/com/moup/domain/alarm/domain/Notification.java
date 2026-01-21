package com.moup.domain.alarm.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Notification {
  Long id;
  Long senderId;
  Long receiverId;
  String title;
  String content;
  LocalDateTime sentAt;
  LocalDateTime readAt;
}
