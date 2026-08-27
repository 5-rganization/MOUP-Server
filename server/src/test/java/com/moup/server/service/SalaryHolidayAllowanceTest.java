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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/// Phase 3-1~3-5 회귀 테스트 — 급여 계산 수정본의 동작을 고정한다.
///
/// [SalaryCalculationServiceTest]가 "수정 후 별도 테스트로 다룬다"고 남긴 항목들이다:
/// C-1(주휴수당 산식) · C-2(분모/분자) · C-4(야간 분 기록) · C-5(야간 분 휴게 제외) ·
/// I-2(`hourly_rate` NULL) · M-1(부동소수점 절삭) · M-2(배분 나머지) · M-8(음수 클램프).
@ExtendWith(MockitoExtension.class)
class SalaryHolidayAllowanceTest {

    private static final Long WORKER_ID = 1L;
    private static final int HOURLY_RATE = 10_000;

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    // ================= C-1 — 법정 산식 =================

    /// 주휴수당 = `min(주 소정근로시간 ÷ 40, 1.0) × 8 × 시급` (주 15시간 이상).
    /// 근로기준법 시행령 제9조 제1항 별표2.
    ///
    /// 예전 산식은 `(주 총 근로시간 ÷ 근무일수) × 시급`으로 **1일 평균 근무시간에 시급을 곱했고
    /// 8시간 상한이 없었다.** 주 5일 근무일 때만 우연히 일치했다.
    @ParameterizedTest(name = "주 {0}시간 / {1}일 근무 → 주휴수당 {2}원 ({3})")
    @DisplayName("주휴수당 법정 산식")
    @CsvSource({
            "40, 5, 80000, '주 5일 8시간 - 예전 산식과 유일하게 일치하는 경우'",
            "20, 2, 40000, '주 2일 10시간 - 예전 산식은 100,000원(2.5배)이었다'",
            "16, 2, 32000, '주 2일 8시간 - 예전 산식은 80,000원(2.5배)'",
            "24, 3, 48000, '주 3일 8시간 - 예전 산식은 80,000원(1.67배)'",
            "48, 4, 80000, '주 48시간 - 8시간 상한이 걸린다. 예전 산식은 120,000원'",
            "14, 2, 0,     '주 15시간 미만 - 주휴수당 없음'",
    })
    void 주휴수당_법정_산식(int weeklyHours, int days, int expectedWeeklyAllowance, String description) {
        LocalDate monday = LocalDate.of(2025, 11, 10);
        int minutesPerDay = weeklyHours * 60 / days;

        List<Work> works = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = monday.plusDays(i);
            LocalDateTime start = date.atTime(9, 0);
            works.add(work((long) (i + 1), date, start, start.plusMinutes(minutesPerDay), 0));
        }

        List<Work> written = runWeek(monday, works);
        int actual = written.stream().mapToInt(Work::getHolidayAllowance).sum();

        assertEquals(expectedWeeklyAllowance, actual, description);
    }

    // ================= C-2 · M-2 — 분모/분자와 나머지 =================

    @Test
    @DisplayName("퇴근 미기록 근무가 섞여도 주휴수당이 증발하지 않는다")
    void 분모와_분자가_같은_집합에서_나온다() {
        LocalDate monday = LocalDate.of(2025, 11, 10);
        List<Work> works = new ArrayList<>();
        // 월~수 8시간씩 (주 24시간) + 목요일은 퇴근 미기록
        for (int i = 0; i < 3; i++) {
            LocalDate date = monday.plusDays(i);
            works.add(work((long) (i + 1), date, date.atTime(9, 0), date.atTime(17, 0), 0));
        }
        LocalDate thursday = monday.plusDays(3);
        works.add(Work.builder().id(4L).workerId(WORKER_ID).workDate(thursday)
                .startTime(thursday.atTime(9, 0)).endTime(null)
                .restTimeMinutes(0).hourlyRate(HOURLY_RATE).build());

        List<Work> written = runWeek(monday, works);
        int total = written.stream().mapToInt(Work::getHolidayAllowance).sum();

        // 주 24시간 → 24/40 × 8 × 10,000 = 48,000원.
        // 예전에는 분모가 4(전체 건수)인데 배분 대상은 3건이라 12,000원이 사라졌다.
        assertEquals(48_000, total, "퇴근 미기록 근무 때문에 주휴수당이 새면 안 된다");
    }

    @Test
    @DisplayName("정수 나눗셈 나머지를 버리지 않는다")
    void 배분_나머지가_보존된다() {
        LocalDate monday = LocalDate.of(2025, 11, 10);
        List<Work> works = new ArrayList<>();
        // 3일 × 5시간 20분 = 주 16시간 → 16/40 × 8 × 10,000 = 32,000원.
        for (int i = 0; i < 3; i++) {
            LocalDate date = monday.plusDays(i);
            works.add(work((long) (i + 1), date, date.atTime(9, 0), date.atTime(14, 20), 0));
        }

        List<Work> written = runWeek(monday, works);
        int total = written.stream().mapToInt(Work::getHolidayAllowance).sum();

        // 32,000 / 3 = 10,666 나머지 2. 나머지를 버리면 합계가 31,998이 된다.
        assertEquals(32_000, total, "나머지가 버려지면 합계가 줄어든다");
    }

    // ================= C-4 · C-5 — 야간 분은 사실 기록 =================

    @Test
    @DisplayName("야간수당을 끄고 일해도 야간 근무시간은 기록된다")
    void 야간_분은_수당_설정과_무관하게_기록된다() {
        LocalDate workDate = LocalDate.of(2025, 11, 10);
        Work result = salaryCalculationService.calculateDailyIncome(
                work(1L, workDate, workDate.atTime(22, 0), workDate.plusDays(1).atTime(6, 0), 0),
                0, false);   // hasNightAllowance = false

        assertAll(
                () -> assertEquals(480, result.getNightWorkMinutes(),
                        "야간 시간은 사실이다. 수당을 끄면 0으로 남아 나중에 켜도 복원되지 않았다"),
                () -> assertEquals(0, result.getNightAllowance(), "수당은 설정대로 0원")
        );
    }

    @Test
    @DisplayName("휴게시간이 야간 근무시간에 비례 배분되어 주간 근무시간이 음수가 되지 않는다")
    void 휴게시간_비례_배분() {
        LocalDate workDate = LocalDate.of(2025, 11, 10);
        // 22:00~06:00 전부 야간 480분, 휴게 60분
        Work result = salaryCalculationService.calculateDailyIncome(
                work(1L, workDate, workDate.atTime(22, 0), workDate.plusDays(1).atTime(6, 0), 60),
                0, true);

        int dayTimeMinutes = result.getNetWorkMinutes() - result.getNightWorkMinutes();
        assertAll(
                () -> assertEquals(420, result.getNetWorkMinutes(), "순 근무시간 = 480 - 60"),
                () -> assertEquals(420, result.getNightWorkMinutes(),
                        "전부 야간이므로 야간 분도 휴게를 뺀 값이어야 한다"),
                () -> assertTrue(dayTimeMinutes >= 0,
                        "주간 근무시간이 음수가 되면 안 된다. 실제=" + dayTimeMinutes)
        );
    }

    @Test
    @DisplayName("휴게시간이 근무시간보다 길어도 음수가 되지 않는다")
    void 휴게가_근무보다_길어도_클램프된다() {
        LocalDate workDate = LocalDate.of(2025, 11, 10);
        Work result = salaryCalculationService.calculateDailyIncome(
                work(1L, workDate, workDate.atTime(9, 0), workDate.atTime(10, 0), 120), 0, true);

        assertAll(
                () -> assertEquals(0, result.getNetWorkMinutes()),
                () -> assertEquals(0, result.getBasePay()),
                () -> assertEquals(0, result.getNightWorkMinutes())
        );
    }

    // ================= M-1 — 부동소수점 절삭 =================

    /// `분 / 60.0 * 시급`은 이진 부동소수점 오차로 값이 1원씩 낮게 떨어졌다.
    /// 절삭 방향은 **항상 근로자에게 불리**했다.
    @ParameterizedTest(name = "시급 {0} × {1}분 → {2}원")
    @DisplayName("기본급이 정수 연산으로 정확히 계산된다")
    @CsvSource({
            "12000, 246, 49200",   // 예전: 49,199
            "15000, 245, 61250",   // 예전: 61,249
            "10000, 480, 80000",
            "10030, 480, 80240",
    })
    void 기본급_정수_연산(int hourlyRate, int minutes, int expectedBasePay) {
        LocalDate workDate = LocalDate.of(2025, 11, 10);
        LocalDateTime start = workDate.atTime(9, 0);
        Work input = Work.builder().id(1L).workerId(WORKER_ID).workDate(workDate)
                .startTime(start).endTime(start.plusMinutes(minutes))
                .restTimeMinutes(0).hourlyRate(hourlyRate).build();

        Work result = salaryCalculationService.calculateDailyIncome(input, 0, false);

        assertEquals(expectedBasePay, result.getBasePay());
    }

    // ================= I-2 — hourly_rate NULL =================

    @Test
    @DisplayName("hourly_rate가 NULL인 레거시 행이 섞여도 500이 나지 않는다")
    void 시급_NULL_행이_있어도_터지지_않는다() {
        LocalDate monday = LocalDate.of(2025, 11, 10);
        List<Work> works = new ArrayList<>();
        // 월요일만 시급 NULL, 나머지는 정상
        LocalDate mon = monday;
        works.add(Work.builder().id(1L).workerId(WORKER_ID).workDate(mon)
                .startTime(mon.atTime(9, 0)).endTime(mon.atTime(17, 0))
                .restTimeMinutes(0).hourlyRate(null).build());
        for (int i = 1; i < 3; i++) {
            LocalDate date = monday.plusDays(i);
            works.add(work((long) (i + 1), date, date.atTime(9, 0), date.atTime(17, 0), 0));
        }

        List<Work> written = runWeek(monday, works);
        int total = written.stream().mapToInt(Work::getHolidayAllowance).sum();

        // 확정 정책 4 — 기준 시급은 그 주 **마지막** 근무의 시급(10,000).
        // 주 24시간 → 24/40 × 8 × 10,000 = 48,000
        assertEquals(48_000, total, "NULL은 건너뛰고 마지막 non-null 시급을 쓴다");
    }

    // ================= 헬퍼 =================

    /// 주 단위 재계산을 돌리고 DB에 쓰인 결과를 돌려준다.
    private List<Work> runWeek(LocalDate monday, List<Work> works) {
        AtomicReference<List<Work>> written = new AtomicReference<>(List.of());
        when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any()))
                .thenAnswer(invocation -> new ArrayList<>(works));
        doAnswer(invocation -> {
            written.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        }).when(workRepository).updateWorkWeekDetailsBatch(any());

        salaryCalculationService.recalculateWorkWeek(WORKER_ID, monday, hourlySalary());
        return written.get();
    }

    private static Work work(Long id, LocalDate workDate, LocalDateTime start, LocalDateTime end, int rest) {
        return Work.builder().id(id).workerId(WORKER_ID).workDate(workDate)
                .startTime(start).endTime(end).restTimeMinutes(rest).hourlyRate(HOURLY_RATE).build();
    }

    private static Salary hourlySalary() {
        return Salary.builder().id(1L).workerId(WORKER_ID)
                .salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
                .hourlyRate(HOURLY_RATE).salaryDate(25)
                .hasNationalPension(false).hasHealthInsurance(false)
                .hasEmploymentInsurance(false).hasIndustrialAccident(false)
                .hasIncomeTax(false).hasHolidayAllowance(true).hasNightAllowance(true)
                .build();
    }
}
