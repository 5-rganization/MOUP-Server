package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@ToString
public class Work {
    private Long id;
    private Long workerId;
    private Long routineId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalDateTime actualStartTime;
    private LocalTime endTime;
    private LocalDateTime actualEndTime;
    private LocalTime restTime;
    private String memo;
    private Integer dailyIncome;
    private Boolean isRepeated;
    private String repeatDays;
    private LocalDate repeatEndDate;
}
