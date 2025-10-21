package com.moup.server.model.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Integer grossWorkMinutes;
    private Integer netWorkMinutes;
    private String memo;
    private Integer hourlyRate;
    private Integer basePay;
    private Integer nightAllowance;
    private Integer holidayAllowance;
    private Integer grossIncome;
    private Integer estimatedNetIncome;
    private String repeatDays;
    private LocalDate repeatEndDate;
}
