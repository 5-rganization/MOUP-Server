package com.moup.domain.workplace.domain;

import com.moup.domain.alarm.domain.AlarmType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkplaceJoinPayload {

  private final String type = AlarmType.WORKPLACE_JOIN_REQUEST.toString();
  private String content;
  private Long workplaceId;
  private Long workerId;
}
