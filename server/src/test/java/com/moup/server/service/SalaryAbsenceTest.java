package com.moup.server.service;

import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.OwnerMonthlyWorkplaceSummaryResponse;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// 스코프 3 Q3 — 주휴수당은 "법대로": 1주 소정근로일을 **개근**해야 발생한다
/// (근로기준법 시행령 제30조 제1항). 결근은 `works.is_absent`로 명시한다.
@ExtendWith(MockitoExtension.class)
class SalaryAbsenceTest {

    private static final Long WORKER_ID = 1L;
    private static final int HOURLY_RATE = 10_000;
    /// 월요일. 월~금 8시간씩 = 주 40시간 → 주휴수당 80,000원.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    @Test
    @DisplayName("결근이 없으면 주휴수당이 나온다 (대조군)")
    void 개근이면_주휴수당이_있다() {
        List<Work> written = runWeek(weekdays(-1));

        assertEquals(80_000, written.stream().mapToInt(Work::getHolidayAllowance).sum());
    }

    @Test
    @DisplayName("결근이 하루라도 있으면 그 주 주휴수당이 없다")
    void 결근이_있으면_주휴수당이_없다() {
        // 수요일 결근. 나머지 4일 32시간은 15시간을 넘지만 개근이 아니다.
        List<Work> written = runWeek(weekdays(2));

        assertEquals(0, written.stream().mapToInt(Work::getHolidayAllowance).sum(),
                "개근 요건을 안 보면 32/40 × 8 × 10,000 = 64,000원이 나간다");
    }

    @Test
    @DisplayName("결근한 근무는 시간도 급여도 0이고, 나머지 근무는 그대로다")
    void 결근한_근무는_0원이다() {
        List<Work> written = runWeek(weekdays(2));

        Work absent = written.get(2);
        Work attended = written.get(0);
        assertAll(
                () -> assertEquals(0, absent.getBasePay()),
                () -> assertEquals(0, absent.getGrossIncome()),
                () -> assertEquals(0, absent.getNetWorkMinutes(), "일하지 않은 시간이 4대보험 시간 기준에 섞이면 안 된다"),
                () -> assertEquals(80_000, attended.getBasePay(), "결근이 다른 날 기본급을 건드리면 안 된다"),
                // 예정 시간은 행에 남는다 — 지우면 무엇을 결근했는지 알 수 없다
                () -> assertEquals(MONDAY.plusDays(2).atTime(9, 0), absent.getStartTime())
        );
    }

    @Test
    @DisplayName("월 공제액을 결근일에 배분하지 않는다")
    void 월_공제액은_출근일에만_배분된다() {
        ReflectionTestUtils.setField(salaryCalculationService, "incomeTaxRate", 0.033);
        // 3일 중 하루 결근 → 세전 160,000원. 소득세 5,280 + 지방소득세 528 = 5,808원.
        List<Work> month = List.of(
                paid(1L, MONDAY, false), paid(2L, MONDAY.plusDays(1), false), paid(3L, MONDAY.plusDays(2), true));
        when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any())).thenReturn(month);

        Salary salary = hourlySalary().hasIncomeTax(true).build();
        salaryCalculationService.recalculateEstimatedNetIncomeForMonth(WORKER_ID, 2026, 10, salary);

        // 결근일까지 세면 5,808 / 3 = 1,936원씩 배분되고, 결근일 몫은 0원짜리 날에 붙어 사라진다.
        verify(workRepository).updateAllEstimatedNetIncomesForMonth(eq(WORKER_ID), any(), any(), eq(2_904));
    }

    @Test
    @DisplayName("일급제 고정급은 결근일을 지급하지 않는다")
    void 일급제_고정급은_결근일을_빼고_센다() {
        Workplace workplace = Workplace.builder().id(20L).ownerId(2L).workplaceName("근무지").build();
        Worker worker = Worker.builder().id(WORKER_ID).userId(3L).workplaceId(20L).isAccepted(true).build();
        Salary dailyFixed = hourlySalary()
                .salaryType(SalaryType.SALARY_DAILY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_FIXED)
                .fixedRate(100_000).build();
        when(workplaceRepository.findAllByOwnerId(2L)).thenReturn(List.of(workplace));
        when(workerRepository.findAllByWorkplaceIdListIn(any())).thenReturn(List.of(worker));
        when(userRepository.findAllByIdListIn(any())).thenReturn(List.of(User.builder().id(3L).nickname("알바").build()));
        when(salaryRepository.findAllByWorkerIdListIn(any())).thenReturn(List.of(dailyFixed));
        when(workRepository.findAllByWorkerIdListInAndDateRange(any(), any(), any())).thenReturn(List.of(
                paid(1L, MONDAY, false), paid(2L, MONDAY.plusDays(1), false), paid(3L, MONDAY.plusDays(2), true)));

        List<OwnerMonthlyWorkplaceSummaryResponse> summary =
                salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(2L, 2026, 10);

        assertEquals(200_000, summary.get(0).getMonthlyWorkerSummaryInfoList().get(0).getGrossIncome(),
                "결근일까지 세면 300,000원이 된다");
    }

    // ================= 헬퍼 =================

    /// 월~금 8시간 근무. `absentIndex`번째 날(0 = 월)을 결근으로 둔다. -1이면 결근 없음.
    private static List<Work> weekdays(int absentIndex) {
        List<Work> works = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate date = MONDAY.plusDays(i);
            works.add(Work.builder().id((long) (i + 1)).workerId(WORKER_ID).workDate(date)
                    .startTime(date.atTime(9, 0)).endTime(date.atTime(17, 0))
                    .restTimeMinutes(0).hourlyRate(HOURLY_RATE)
                    .hasNightAllowance(true).hasHolidayAllowance(true)
                    .isAbsent(i == absentIndex)
                    .build());
        }
        return works;
    }

    /// 이미 계산이 끝난 근무 행. 결근이면 급여 컬럼이 0이다.
    private static Work paid(Long id, LocalDate date, boolean absent) {
        return Work.builder().id(id).workerId(WORKER_ID).workDate(date)
                .startTime(date.atTime(9, 0)).endTime(date.atTime(17, 0))
                .restTimeMinutes(0).hourlyRate(HOURLY_RATE)
                .netWorkMinutes(absent ? 0 : 480)
                .grossIncome(absent ? 0 : 80_000)
                .isAbsent(absent)
                .build();
    }

    private List<Work> runWeek(List<Work> works) {
        AtomicReference<List<Work>> written = new AtomicReference<>(List.of());
        when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any()))
                .thenAnswer(invocation -> new ArrayList<>(works));
        doAnswer(invocation -> {
            written.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        }).when(workRepository).updateWorkWeekDetailsBatch(any());

        salaryCalculationService.recalculateWorkWeek(WORKER_ID, MONDAY, hourlySalary().build());
        return written.get();
    }

    private static Salary.SalaryBuilder hourlySalary() {
        return Salary.builder().id(1L).workerId(WORKER_ID)
                .salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
                .hourlyRate(HOURLY_RATE).salaryDate(25)
                .hasNationalPension(false).hasHealthInsurance(false)
                .hasEmploymentInsurance(false).hasIndustrialAccident(false)
                .hasIncomeTax(false).hasHolidayAllowance(true).hasNightAllowance(true);
    }
}
