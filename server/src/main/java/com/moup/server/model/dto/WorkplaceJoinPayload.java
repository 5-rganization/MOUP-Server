package com.moup.server.model.dto;

import com.moup.server.common.AlarmType;
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
