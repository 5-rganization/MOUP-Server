package com.moup.server.service;

import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.mapper.PublicHolidayRepository;
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

/// 휴일근로 가산 (Phase 11 Q6) — 근로기준법 제56조 제2항: 8시간까지 50%, 넘는 분은 100%.
/// 기대값은 손으로 센 분 × 시급 10,030 ÷ 120(= 50%)이다.
@ExtendWith(MockitoExtension.class)
class SalaryPublicHolidayTest {

    private static final Long WORKER_ID = 1L;
    private static final int RATE = 10_030;
    /// 2026-10-12(월) 주. 이 주에는 실제 공휴일이 없어 테스트가 지정한 날만 공휴일이 된다.
    private static final LocalDate MON = LocalDate.of(2026, 10, 12);
    private static final LocalDate FRI = MON.plusDays(4);

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private PublicHolidayRepository publicHolidayRepository;

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    private static Work work(long id, LocalDateTime start, LocalDateTime end, boolean hasPublicHoliday) {
        return Work.builder().id(id).workerId(WORKER_ID).workDate(start.toLocalDate())
                .startTime(start).endTime(end).restTimeMinutes(0).hourlyRate(RATE)
                .hasNightAllowance(true).hasHolidayAllowance(false)
                .hasOvertimeAllowance(true).hasPublicHolidayAllowance(hasPublicHoliday)
                .isAbsent(false).build();
    }

    private static Work on(long id, LocalDate date, int fromHour, int toHour) {
        return work(id, date.atTime(fromHour, 0), date.atTime(toHour, 0), true);
    }

    private Map<Long, Work> recalculate(List<LocalDate> holidays, List<Work> week) {
        AtomicReference<List<Work>> written = new AtomicReference<>(List.of());
        // 실제 쿼리처럼 범위로 거른다. 범위를 무시하면 "다음 주 월요일까지 읽는다"를 검증할 수 없다.
        when(publicHolidayRepository.findDatesBetween(any(), any())).thenAnswer(i -> {
            LocalDate from = i.getArgument(0);
            LocalDate to = i.getArgument(1);
            return holidays.stream().filter(d -> !d.isBefore(from) && !d.isAfter(to)).toList();
        });
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

    private static int half(long minutes) {
        return (int) ((minutes * RATE + 60) / 120);
    }

    @Test
    @DisplayName("공휴일 8시간 근무에는 50%가 붙는다")
    void 공휴일_8시간() {
        Work result = recalculate(List.of(FRI), List.of(on(1, FRI, 9, 17))).get(1L);

        assertEquals(480, result.getPublicHolidayWorkMinutes());
        assertEquals(half(480), result.getPublicHolidayAllowance());     // 40,120원
        assertEquals(8 * RATE + half(480), result.getGrossIncome());
    }

    /// 휴일근로 가산이 그 시간의 가산이다. 연장으로 또 세면 같은 분에 50%가 두 번 붙는다.
    @Test
    @DisplayName("공휴일 10시간이면 8시간 넘는 2시간은 100%이고, 연장으로 따로 세지 않는다")
    void 공휴일_8시간_초과() {
        Work result = recalculate(List.of(FRI), List.of(on(1, FRI, 9, 19))).get(1L);

        assertEquals(600, result.getPublicHolidayWorkMinutes());
        assertEquals(half(600) + half(120), result.getPublicHolidayAllowance());
        assertEquals(0, result.getOvertimeWorkMinutes());
    }

    @Test
    @DisplayName("공휴일 전날 밤 근무는 자정 이후만 공휴일이다 (야간과 함께 붙는다)")
    void 자정_넘는_근무() {
        LocalDate thu = FRI.minusDays(1);
        Work result = recalculate(List.of(FRI),
                List.of(work(1, thu.atTime(22, 0), FRI.atTime(6, 0), true))).get(1L);

        assertEquals(360, result.getPublicHolidayWorkMinutes());   // 00~06시
        assertEquals(480, result.getNightWorkMinutes());
        assertEquals(8 * RATE + half(480) + half(360), result.getGrossIncome());
    }

    @Test
    @DisplayName("공휴일 가산을 끄면 시간은 기록하되 수당은 0이다")
    void 가산_꺼짐() {
        Work result = recalculate(List.of(FRI),
                List.of(work(1, FRI.atTime(9, 0), FRI.atTime(17, 0), false))).get(1L);

        assertEquals(480, result.getPublicHolidayWorkMinutes());
        assertEquals(0, result.getPublicHolidayAllowance());
    }

    /// 공휴일 시간을 주 40시간 판정에 넣으면 토요일 8시간이 연장이 되어, 같은 주에 휴일 가산과 연장 가산이 겹친다.
    @Test
    @DisplayName("공휴일 근무시간은 주 40시간 연장 판정에서 빠진다")
    void 주_40시간에서_제외() {
        List<Work> week = new ArrayList<>();
        for (int d = 0; d < 6; d++) {
            week.add(on(d + 1, MON.plusDays(d), 9, 17));   // 월~토 8시간, 금요일이 공휴일
        }
        Map<Long, Work> result = recalculate(List.of(FRI), week);

        assertEquals(0, result.values().stream().mapToInt(Work::getOvertimeWorkMinutes).sum());
        assertEquals(480, result.get(5L).getPublicHolidayWorkMinutes());
    }

    @Test
    @DisplayName("공휴일이 아닌 날은 휴일 시간이 0이다")
    void 평일() {
        Work result = recalculate(List.of(), List.of(on(1, FRI, 9, 17))).get(1L);

        assertEquals(0, result.getPublicHolidayWorkMinutes());
        assertEquals(0, result.getPublicHolidayAllowance());
    }

    @Test
    @DisplayName("일요일 밤에 시작해 공휴일인 다음 주 월요일로 넘어가는 근무도 자정 이후는 공휴일이다")
    void 다음_주_월요일_공휴일() {
        LocalDate sun = MON.plusDays(6);
        Work result = recalculate(List.of(sun.plusDays(1)),
                List.of(work(1, sun.atTime(22, 0), sun.plusDays(1).atTime(6, 0), true))).get(1L);

        assertEquals(360, result.getPublicHolidayWorkMinutes());
    }

    @Test
    @DisplayName("근무 저장·주 재계산·급여 저장 SQL이 휴일 컬럼을 쓰고, 공휴일 조회가 매핑된다")
    void SQL() {
        Configuration configuration = new Configuration();
        configuration.addMapper(WorkRepository.class);
        configuration.addMapper(SalaryRepository.class);
        configuration.addMapper(PublicHolidayRepository.class);
        Map<String, Object> params = Map.of("worksToUpdate", List.of(Work.builder().id(1L).build()),
                "works", List.of(Work.builder().id(1L).build()));
        Function<String, String> sql = id -> configuration.getMappedStatement(id).getSqlSource().getBoundSql(params).getSql();
        String work = WorkRepository.class.getName() + ".";
        String salary = SalaryRepository.class.getName() + ".";

        assertAll(
                () -> assertTrue(sql.apply(work + "create").contains("has_public_holiday_allowance")),
                () -> assertTrue(sql.apply(work + "createBatch").contains("has_public_holiday_allowance")),
                () -> assertTrue(sql.apply(work + "updateWorkWeekDetailsBatch").contains("public_holiday_allowance =")),
                () -> assertTrue(sql.apply(work + "updateWorkWeekDetailsBatch").contains("public_holiday_work_minutes =")),
                () -> assertTrue(sql.apply(work + "updateSalarySnapshotFrom").contains("has_public_holiday_allowance")),
                () -> assertTrue(sql.apply(salary + "create").contains("has_public_holiday_allowance")),
                () -> assertTrue(sql.apply(salary + "update").contains("has_public_holiday_allowance")),
                () -> assertTrue(sql.apply(PublicHolidayRepository.class.getName() + ".findDatesBetween").contains("BETWEEN"))
        );
    }
}
