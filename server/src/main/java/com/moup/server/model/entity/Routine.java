package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalTime;

@Getter
@Builder
@ToString
public class Routine {
    private Long id;
    private Long userId;
    private String routineName;
    private LocalTime alarmTime;
}
