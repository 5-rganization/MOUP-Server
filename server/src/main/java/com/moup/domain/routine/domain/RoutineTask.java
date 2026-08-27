package com.moup.domain.routine.domain;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RoutineTask {
    private Long id;
    private Long routineId;
    private String content;
    private Integer orderIndex;
}
