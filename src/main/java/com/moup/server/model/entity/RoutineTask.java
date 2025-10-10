package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class RoutineTask {
    private Long id;
    private Long routineId;
    private String content;
    private Integer orderIndex;
}
