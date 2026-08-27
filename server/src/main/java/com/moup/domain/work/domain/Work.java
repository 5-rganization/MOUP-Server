package com.moup.domain.work.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer nightWorkMinutes;
    private String memo;
    private Integer hourlyRate;
    /// 등록 시점의 수당 적용 여부 스냅샷 (확정 정책 3).
    private Boolean hasNightAllowance;
    private Boolean hasHolidayAllowance;
    private Integer basePay;
    private Integer nightAllowance;
    private Integer holidayAllowance;
    private Integer grossIncome;
    private Integer estimatedNetIncome;
    private String repeatGroupId;
}
