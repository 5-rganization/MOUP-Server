package com.moup.domain.salary.application;

import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.dto.SalaryUpdateRequest;
import com.moup.domain.salary.exception.SalaryWorkerNotFoundException;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.global.error.InvalidFieldFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

/// 급여 설정 수정의 단일 진입점. 알바생(근무지 수정·근무자 수정)과 사장님 경로가 모두 여기를 쓴다.
///
/// 예전에는 세 경로가 같은 코드를 복붙해 각자 급여를 통째로 덮어쓰고 이번 달 1일부터 공제만 다시 계산했다.
/// 근무 행의 시급·수당 스냅샷은 바꾸지 않아, **시급을 올려도 이미 잡아 둔 근무는 옛 시급 그대로**였다.
@Service
@RequiredArgsConstructor
public class SalaryUpdateService {

    private final SalaryRepository salaryRepository;
    private final WorkRepository workRepository;
    private final SalaryCalculationService salaryCalculationService;

    /// 보낸 필드만 반영하고, 적용 시작일 이후 근무의 스냅샷을 새 설정으로 바꿔 다시 계산한다.
    ///
    /// @return 설정이 실제로 바뀌었으면 `true`. 같은 값을 다시 보낸 경우 아무것도 하지 않고 `false`다
    ///         (호출자가 변경 알림을 보낼지 정하는 데 쓴다).
    @Transactional
    public boolean update(Long workerId, SalaryUpdateRequest request) {
        Salary current = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new);
        Salary updated = request.applyTo(current);
        if (!updated.isRateConsistentWithCalculation()) {
            throw new InvalidFieldFormatException("시급제는 시급을, 고정급제는 고정급을 입력해야 합니다.");
        }
        if (updated.equals(current)) {
            return false;
        }
        salaryRepository.update(updated);

        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
        LocalDate applyFrom = request.resolveApplyFrom().startDate(today);

        // 확정 정책 3 — 근무 행이 들고 있는 설정을 적용 시작일 이후만 갈아 끼운다. 그 이전 근무는 그대로다.
        // 고정급은 시급이 없다. 근무 등록과 같이 0을 넣는다(스코프 3 I-7). `hourlyRate`만 보면 안 된다 —
        // 부분 수정은 생략한 필드를 유지하므로 시급제에서 고정급으로 바꿔도 옛 시급이 남아 있다.
        boolean isHourly = updated.getSalaryCalculation() == SalaryCalculation.SALARY_CALCULATION_HOURLY;
        workRepository.updateSalarySnapshotFrom(workerId, applyFrom,
                isHourly ? updated.getHourlyRate() : 0,
                Boolean.TRUE.equals(updated.getHasNightAllowance()),
                Boolean.TRUE.equals(updated.getHasHolidayAllowance()),
                Boolean.TRUE.equals(updated.getHasOvertimeAllowance()));

        // 적용 시작일이 든 주부터 다시 계산한다. 그 주의 앞쪽 근무도 주휴수당 배분이 같이 바뀐다.
        // 주마다 월요일이 아니라 **그 주의 첫 근무일**을 넘긴다. recalculateWorkWeek은 넘긴 날짜의 달도
        // 다시 계산하는데, 월요일을 넘기면 주가 월 경계에 걸칠 때 지난달 공제까지 새 설정으로 바뀐다.
        Map<LocalDate, LocalDate> firstDateByWeek = new LinkedHashMap<>();
        for (LocalDate date : workRepository.findWorkDatesFrom(workerId, applyFrom)) {
            firstDateByWeek.putIfAbsent(date.with(DayOfWeek.MONDAY), date);
        }
        for (LocalDate firstDate : firstDateByWeek.values()) {
            salaryCalculationService.recalculateWorkWeek(workerId, firstDate, updated);
        }

        // 공제(4대보험·세금)는 월 단위라 적용 시작일과 무관하게 그 달 전체가 새 설정을 따른다.
        // 오늘부터 적용해도 이번 달 앞쪽 근무의 추정 실수령액이 바뀌는 것은 이 때문이다.
        for (WorkRepository.WorkMonthDto month :
                workRepository.findDistinctWorkMonthsAfter(workerId, applyFrom.withDayOfMonth(1))) {
            salaryCalculationService.recalculateEstimatedNetIncomeForMonth(
                    workerId, month.year(), month.month(), updated);
        }
        return true;
    }
}
