package com.moup.domain.routine.domain;

import lombok.*;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Routine {
    private Long id;
    private Long userId;
    private String routineName;
    private LocalTime alarmTime;
}
