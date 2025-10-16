package com.moup.server.model.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Announcement {
  String title;
  String content;
  LocalDateTime sentAt;
}
