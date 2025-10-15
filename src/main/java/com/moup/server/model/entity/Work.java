package com.moup.server.model.entity;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@ToString
public class Work {
    private Long id;
    private Long workerId;
    private LocalDate workDate;
    private LocalDateTime startTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private Integer restTimeMinutes;
    private String memo;
    private Integer hourlyRate;
    private Integer basePay;
    private Integer nightAllowance;
    private Integer holidayAllowance;
    private Integer grossIncome;
    private Integer estimatedNetIncome;
    @Getter(AccessLevel.NONE)
    private String repeatDays;
    private LocalDate repeatEndDate;

    public static class WorkBuilder {
        public WorkBuilder repeatDays(List<DayOfWeek> days) {
            if (days == null || days.isEmpty()) {
                repeatDays = null;
            } else {
                repeatDays = days.stream()
                        .map(DayOfWeek::name)
                        .collect(Collectors.joining(","));
            }

            return this;
        }
    }

    public List<DayOfWeek> getRepeatDays() {
        if (this.repeatDays == null || this.repeatDays.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(this.repeatDays.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .toList(); // .collect(Collectors.toList())와 동일
    }
}
