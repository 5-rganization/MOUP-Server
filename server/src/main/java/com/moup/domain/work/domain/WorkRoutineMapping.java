package com.moup.domain.work.domain;

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
