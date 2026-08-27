package com.moup.domain.routine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/// 근무별 할 일 완료(체크) 상태.
///
/// 같은 루틴이 여러 근무에 연결되므로 완료는 반드시 (근무, 할 일) 쌍이다.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineTaskCompletion {
    private Long id;
    private Long workId;
    private Long routineTaskId;
    private LocalDateTime completedAt;
}
