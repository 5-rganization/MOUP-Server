package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class WorkRoutineMapping {
    private Long id;
    private Long workId;
    private Long routineId;
}
