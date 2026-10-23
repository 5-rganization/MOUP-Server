package com.moup.server.service;

import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.WorkerMonthlyWorkplaceSummaryResponse;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/// 스코프 3 Q7 — 원 단위 처리 규칙.
/// - **지급 항목**(기본급·야간수당·주휴수당)은 원 단위 **반올림**
/// - **공제 항목**(4대보험·세금)은 각 공단·국세청 고지 방식대로 **10원 미만 절사**
///
/// 기대값은 코드와 같은 산술을 쓰지 않도록 분수(정확한 값)로 따로 계산해 넣었다.
@ExtendWith(MockitoExtension.class)
class SalaryRoundingTest {

    private static final Long USER_ID = 7L;
    private static final Long WORKER_ID = 1L;
    private static final int HOURLY_RATE = 10_030;
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    // ================= 지급 — 반올림 =================

    @ParameterizedTest(name = "시급 10,030 × {0}분 → 기본급 {1}원 ({2})")
    @DisplayName("기본급은 원 단위 반올림한다")
    @CsvSource({
            "5, 836, '835.83 — 예전에는 버려서 835원'",
            "3, 502, '501.5 — 정확히 0.5면 올린다'",
            "1, 167, '167.17 — 내림 방향도 맞다'",
    })
    void 기본급_반올림(int minutes, int expectedBasePay, String description) {
        LocalDateTime start = MONDAY.atTime(9, 0);
        Work result = salaryCalculationService.calculateDailyIncome(
                work(1L, start, start.plusMinutes(minutes)), 0, false);

        assertEquals(expectedBasePay, result.getBasePay(), description);
    }

    @Test
    @DisplayName("야간수당은 원 단위 반올림한다")
    void 야간수당_반올림() {
        LocalDateTime start = MONDAY.atTime(22, 0);
        Work result = salaryCalculationService.calculateDailyIncome(
                work(1L, start, start.plusMinutes(1)), 0, true);

        // 10,030 / 120 = 83.58 → 84원. 예전에는 83원.
        assertEquals(84, result.getNightAllowance());
    }

    @Test
    @DisplayName("주휴수당은 원 단위 반올림한다")
    void 주휴수당_반올림() {
        // 주 1,001분 → 1001/2400 × 8 × 10,030 = 33,466.77원 → 33,467원. 예전에는 33,466원.
        LocalDateTime mon = MONDAY.atTime(9, 0);
        LocalDateTime tue = MONDAY.plusDays(1).atTime(9, 0);
        List<Work> works = List.of(
                work(1L, mon, mon.plusMinutes(500)),
                work(2L, tue, tue.plusMinutes(501)));

        AtomicReference<List<Work>> written = new AtomicReference<>(List.of());
        when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any()))
                .thenAnswer(invocation -> new ArrayList<>(works));
        doAnswer(invocation -> {
            written.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        }).when(workRepository).updateWorkWeekDetailsBatch(any());

        salaryCalculationService.recalculateWorkWeek(WORKER_ID, MONDAY, salary(false).build());

        assertEquals(33_467, written.get().stream().mapToInt(Work::getHolidayAllowance).sum());
    }

    // ================= 공제 — 10원 미만 절사 =================

    @Test
    @DisplayName("공제 항목은 전부 10원 미만 절사한다")
    void 공제_10원_미만_절사() {
        setRates();
        // 세전 1,234,567원, 월 60시간(4대보험 적용 대상)
        WorkerMonthlyWorkplaceSummaryResponse summary = workerSummary(1_234_567);

        assertAll(
                // 58,641.93 → 58,640 (예전 int 변환은 58,641)
                () -> assertEquals(58_640, summary.getNationalPension()),
                // 건강 44,382.68 → 44,380, 장기요양은 **절사된 건강보험료** 44,380 × 13.14% = 5,831.53 → 5,830
                () -> assertEquals(50_210, summary.getHealthInsurance()),
                // 11,111.10 → 11,110
                () -> assertEquals(11_110, summary.getEmploymentInsurance()),
                // 40,740.71 → 40,740
                () -> assertEquals(40_740, summary.getIncomeTax()),
                // 지방소득세 4,074 → 4,070까지 합친 공제 164,770원
                () -> assertEquals(1_069_797, summary.getNetIncome())
        );
    }

    @Test
    @DisplayName("일의 자리가 5 이상이어도 공제는 올리지 않는다")
    void 공제는_반올림하지_않는다() {
        setRates();
        // 세전 1,000,556원 — 모든 항목의 일의 자리가 5 이상이라 반올림했다면 전부 10원씩 올라간다.
        WorkerMonthlyWorkplaceSummaryResponse summary = workerSummary(1_000_556);

        assertAll(
                () -> assertEquals(47_520, summary.getNationalPension()),     // 47,526.41
                () -> assertEquals(40_680, summary.getHealthInsurance()),     // 35,969.99 → 35,960 + 장기요양 4,720
                () -> assertEquals(9_000, summary.getEmploymentInsurance()),  // 9,005.00
                () -> assertEquals(33_010, summary.getIncomeTax()),           // 33,018.35
                () -> assertEquals(867_046, summary.getNetIncome())           // 지방소득세 3,301.0 → 3,300 포함
        );
    }

    @Test
    @DisplayName("10원 단위 경계에서 부동소수점 오차로 10원이 사라지지 않는다")
    void 부동소수점_경계() {
        setRates();
        // 50,000 × 0.009는 정확히 450원인데 double로는 449.99999999999994다.
        // 그대로 10원 단위로 자르면 440원이 된다.
        WorkerMonthlyWorkplaceSummaryResponse summary = workerSummary(50_000);

        assertEquals(450, summary.getEmploymentInsurance());
    }

    // ================= 헬퍼 =================

    private void setRates() {
        ReflectionTestUtils.setField(salaryCalculationService, "nationalPensionRate", 0.0475);
        ReflectionTestUtils.setField(salaryCalculationService, "healthInsuranceRate", 0.03595);
        ReflectionTestUtils.setField(salaryCalculationService, "longTermCareInsuranceRate", 0.1314);
        ReflectionTestUtils.setField(salaryCalculationService, "employmentInsuranceRate", 0.009);
        ReflectionTestUtils.setField(salaryCalculationService, "incomeTaxRate", 0.033);
        ReflectionTestUtils.setField(salaryCalculationService, "insuranceMinHours", 60);
    }

    /// 세전 `gross`원, 순 근무 60시간짜리 한 달을 알바생 홈 요약으로 돌린다.
    private WorkerMonthlyWorkplaceSummaryResponse workerSummary(int gross) {
        Worker worker = Worker.builder().id(WORKER_ID).userId(USER_ID).workplaceId(20L)
                .isAccepted(true).isNowWorking(false).build();
        Workplace workplace = Workplace.builder().id(20L).ownerId(2L).workplaceName("근무지").build();
        Work paid = Work.builder().id(1L).workerId(WORKER_ID).workDate(MONDAY)
                .netWorkMinutes(60 * 60).nightWorkMinutes(0).restTimeMinutes(0)
                .basePay(gross).nightAllowance(0).holidayAllowance(0).grossIncome(gross)
                .build();
        when(workerRepository.findAllByUserId(USER_ID)).thenReturn(List.of(worker));
        when(salaryRepository.findAllByWorkerIdListIn(any())).thenReturn(List.of(salary(true).build()));
        when(workRepository.findAllByWorkerIdListInAndDateRange(any(), any(), any())).thenReturn(List.of(paid));
        when(workplaceRepository.findAllByIdListIn(any())).thenReturn(List.of(workplace));

        return salaryCalculationService.getWorkerMonthlyWorkplaceSummaryList(USER_ID, 2026, 10).get(0);
    }

    private static Work work(Long id, LocalDateTime start, LocalDateTime end) {
        return Work.builder().id(id).workerId(WORKER_ID).workDate(start.toLocalDate())
                .startTime(start).endTime(end).restTimeMinutes(0).hourlyRate(HOURLY_RATE)
                .hasNightAllowance(true).hasHolidayAllowance(true).isAbsent(false)
                .build();
    }

    private static Salary.SalaryBuilder salary(boolean allDeductions) {
        return Salary.builder().id(1L).workerId(WORKER_ID)
                .salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
                .hourlyRate(HOURLY_RATE).salaryDate(25)
                .hasNationalPension(allDeductions).hasHealthInsurance(allDeductions)
                .hasEmploymentInsurance(allDeductions).hasIndustrialAccident(false)
                .hasIncomeTax(allDeductions).hasHolidayAllowance(true).hasNightAllowance(true);
    }
}
