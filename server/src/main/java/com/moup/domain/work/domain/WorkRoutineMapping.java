package com.moup.domain.work.domain;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WorkRoutineMapping {
    private Long id;
    private Long workId;
    private Long routineId;
}
