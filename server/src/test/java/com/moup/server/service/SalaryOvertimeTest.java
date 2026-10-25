package com.moup.server.service;

import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 연장근로 가산 (Phase 11 D11-2) — 근로기준법 제56조: 1일 8시간·1주 40시간 초과분에 50%.
///
/// 기대값은 손으로 센 분 수 × 시급 10,030 ÷ 120(= 50%를 분 단위로)이다.
@ExtendWith(MockitoExtension.class)
class SalaryOvertimeTest {

    private static final Long WORKER_ID = 1L;
    private static final int RATE = 10_030;
    private static final LocalDate MON = LocalDate.of(2026, 10, 5);

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    private static Work work(long id, LocalDateTime start, LocalDateTime end, boolean hasOvertime) {
        return Work.builder().id(id).workerId(WORKER_ID).workDate(start.toLocalDate())
                .startTime(start).endTime(end).restTimeMinutes(0).hourlyRate(RATE)
                .hasNightAllowance(true).hasHolidayAllowance(false).hasOvertimeAllowance(hasOvertime)
                .isAbsent(false).build();
    }

    private static Work day(long id, int dayOffset, int fromHour, int toHour) {
        LocalDate date = MON.plusDays(dayOffset);
        LocalDateTime end = toHour == 24 ? date.plusDays(1).atStartOfDay() : date.atTime(toHour, 0);
        return work(id, date.atTime(fromHour, 0), end, true);
    }

    /// 주를 다시 계산하고 근무별 결과를 id로 돌려준다.
    private Map<Long, Work> recalculate(List<Work> week) {
        AtomicReference<List<Work>> written = new AtomicReference<>(List.of());
        when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any())).thenAnswer(i -> new ArrayList<>(week));
        doAnswer(i -> {
            written.set(new ArrayList<>(i.getArgument(0)));
            return null;
        }).when(workRepository).updateWorkWeekDetailsBatch(any());

        Salary hourly = Salary.builder().workerId(WORKER_ID).salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY).hourlyRate(RATE).build();
        salaryCalculationService.recalculateWorkWeek(WORKER_ID, MON, hourly);
        return written.get().stream().collect(Collectors.toMap(Work::getId, Function.identity()));
    }

    private static int allowance(long minutes) {
        return (int) ((minutes * RATE + 60) / 120);
    }

    @Test
    @DisplayName("하루 10시간이면 8시간을 넘는 2시간이 연장이다")
    void 하루_8시간_초과() {
        Work result = recalculate(List.of(day(1, 0, 9, 19))).get(1L);

        assertEquals(120, result.getOvertimeWorkMinutes());
        assertEquals(10_030, result.getOvertimeAllowance());   // 120분 × 10,030 ÷ 120
        assertEquals(10 * RATE + 10_030, result.getGrossIncome());
    }

    /// 연장 시간은 야간 시간처럼 사실 기록이다. 수당을 나중에 켜도 시간은 이미 남아 있다.
    @Test
    @DisplayName("연장 가산을 끄면 시간은 기록하되 수당은 0이다")
    void 가산_꺼짐() {
        LocalDateTime start = MON.atTime(9, 0);
        Work result = recalculate(List.of(work(1, start, start.plusHours(10), false))).get(1L);

        assertEquals(120, result.getOvertimeWorkMinutes());
        assertEquals(0, result.getOvertimeAllowance());
    }

    @Test
    @DisplayName("같은 날 근무가 둘이면 합쳐서 8시간을 넘는 분이 나중 근무에 붙는다")
    void 같은_날_두_근무() {
        Map<Long, Work> result = recalculate(List.of(day(1, 0, 9, 13), day(2, 0, 14, 20)));

        assertEquals(0, result.get(1L).getOvertimeWorkMinutes());
        assertEquals(120, result.get(2L).getOvertimeWorkMinutes());
    }

    @Test
    @DisplayName("하루 8시간씩 주 6일이면 40시간을 넘는 토요일 8시간이 연장이다")
    void 주_40시간_초과() {
        List<Work> week = new ArrayList<>();
        for (int d = 0; d < 6; d++) {
            week.add(day(d + 1, d, 9, 17));
        }
        Map<Long, Work> result = recalculate(week);

        for (long id = 1; id <= 5; id++) {
            assertEquals(0, result.get(id).getOvertimeWorkMinutes(), "평일 " + id);
        }
        assertEquals(480, result.get(6L).getOvertimeWorkMinutes());
        assertEquals(allowance(480), result.get(6L).getOvertimeAllowance());   // 40,120원
    }

    /// 1일 초과분을 뗀 나머지로 주 40시간을 판정한다. 그렇지 않으면 같은 시간이 두 번 가산된다.
    @Test
    @DisplayName("1일 초과분과 주 초과분을 이중으로 세지 않는다")
    void 이중_가산_없음() {
        List<Work> week = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            week.add(day(d + 1, d, 9, 18));   // 9시간 × 5 = 1일 초과 60분 × 5
        }
        week.add(day(6, 5, 9, 13));           // 토요일 4시간
        Map<Long, Work> result = recalculate(week);

        // 평일: 각 60분(1일 초과). 나머지 8시간 × 5 = 40시간이 소정, 토요일 4시간이 전부 주 초과.
        for (long id = 1; id <= 5; id++) {
            assertEquals(60, result.get(id).getOvertimeWorkMinutes(), "평일 " + id);
        }
        assertEquals(240, result.get(6L).getOvertimeWorkMinutes());
        int total = result.values().stream().mapToInt(Work::getOvertimeWorkMinutes).sum();
        assertEquals(540, total, "45+4=49시간 중 연장은 9시간이다");
    }

    @Test
    @DisplayName("결근한 날은 일하지 않았으므로 주 40시간 판정에서 빠진다")
    void 결근_제외() {
        List<Work> week = new ArrayList<>();
        for (int d = 0; d < 6; d++) {
            week.add(day(d + 1, d, 9, 17));
        }
        // 월요일 결근. 결근을 세면 40시간 초과분 8시간이 토요일(정상 근무)에 붙는다.
        // 토요일을 결근으로 두면 잘못 센 연장이 결근한 근무 자신에게 붙었다가 0원 처리에 지워져 검증이 안 된다.
        week.set(0, week.get(0).toBuilder().isAbsent(true).build());
        Map<Long, Work> result = recalculate(week);

        assertEquals(0, result.values().stream().mapToInt(Work::getOvertimeWorkMinutes).sum());
    }

    /// 가산 사유가 다르므로 겹치면 둘 다 붙는다(근로기준법 제56조 제1항·제3항).
    @Test
    @DisplayName("야간이면서 연장인 시간에는 두 가산이 모두 붙는다")
    void 야간_연장_중복() {
        Work result = recalculate(List.of(day(1, 0, 14, 24))).get(1L);   // 14~24시, 마지막 2시간이 야간이자 연장

        assertEquals(120, result.getNightWorkMinutes());
        assertEquals(120, result.getOvertimeWorkMinutes());
        assertEquals(10 * RATE + allowance(120) + allowance(120), result.getGrossIncome());
    }

    /// 계산이 맞아도 저장 SQL에 컬럼이 빠지면 조용히 0으로 남는다. 렌더링된 SQL로 고정한다.
    @Test
    @DisplayName("근무 저장·주 재계산·급여 저장 SQL이 연장 컬럼을 쓴다")
    void SQL에_연장_컬럼() {
        Configuration configuration = new Configuration();
        configuration.addMapper(WorkRepository.class);
        configuration.addMapper(SalaryRepository.class);
        java.util.function.BiFunction<Class<?>, String, String> sql = (mapper, id) -> configuration
                .getMappedStatement(mapper.getName() + "." + id).getSqlSource()
                .getBoundSql(Map.of("worksToUpdate", List.of(Work.builder().id(1L).build()),
                        "works", List.of(Work.builder().id(1L).build())))
                .getSql();

        assertAll(
                () -> assertTrue(sql.apply(WorkRepository.class, "create").contains("has_overtime_allowance")),
                () -> assertTrue(sql.apply(WorkRepository.class, "createBatch").contains("has_overtime_allowance")),
                () -> assertTrue(sql.apply(WorkRepository.class, "updateWorkWeekDetailsBatch").contains("overtime_allowance =")),
                () -> assertTrue(sql.apply(WorkRepository.class, "updateWorkWeekDetailsBatch").contains("overtime_work_minutes =")),
                () -> assertTrue(sql.apply(WorkRepository.class, "updateSalarySnapshotFrom").contains("has_overtime_allowance")),
                () -> assertTrue(sql.apply(SalaryRepository.class, "create").contains("has_overtime_allowance")),
                () -> assertTrue(sql.apply(SalaryRepository.class, "update").contains("has_overtime_allowance"))
        );
    }
}
