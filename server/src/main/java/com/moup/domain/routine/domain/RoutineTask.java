package com.moup.domain.routine.domain;

import lombok.Builder;
import lombok.Getter;
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
