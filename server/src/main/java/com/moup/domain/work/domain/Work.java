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
    /// 연장 근무시간(분) — 1일 8시간·주 40시간 초과분. 수당 설정과 무관한 사실 기록이다.
    private Integer overtimeWorkMinutes;
    /// 공휴일 근무시간(분). 사실 기록이다.
    private Integer publicHolidayWorkMinutes;
    private String memo;
    private Integer hourlyRate;
    /// 등록 시점의 수당 적용 여부 스냅샷 (확정 정책 3).
    private Boolean hasNightAllowance;
    private Boolean hasHolidayAllowance;
    private Boolean hasOvertimeAllowance;
    private Boolean hasPublicHolidayAllowance;
    /// 결근. 결근한 근무는 급여 0원이고 그 주는 주휴수당이 없다 (개근 요건).
    private Boolean isAbsent;
    private Integer basePay;
    private Integer nightAllowance;
    private Integer holidayAllowance;
    private Integer overtimeAllowance;
    private Integer publicHolidayAllowance;
    private Integer grossIncome;
    private Integer estimatedNetIncome;
    private String repeatGroupId;
}
