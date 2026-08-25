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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/// 급여 계산의 **현재 정확한 동작**을 고정하는 회귀 테스트.
///
/// 코드 리뷰(스코프 3)에서 야간 시간대 판정식과 주 단위 재계산 멱등성이 정확하다고
/// 확인됐다. 이어질 C-1(주휴수당 산식) · C-4(야간 분 기록) · C-5(야간 분 휴게 제외)
/// 수정 과정에서 가장 깨뜨리기 쉬운 두 지점이므로 **수정 전에 먼저 고정**한다.
///
/// 여기서 검증하지 않는 것 — 전부 알려진 결함이며 수정 후 별도 테스트로 다룬다:
/// - 주휴수당 금액 (C-1: 산식 자체가 근무일수에 따라 결과가 뒤집힘)
/// - `hasNightAllowance = false`일 때의 `nightWorkMinutes` (C-4: 사실 기록이 정책에 종속)
/// - 휴게시간이 있는 근무의 `nightWorkMinutes` (C-5: 휴게가 제외되지 않음)
@ExtendWith(MockitoExtension.class)
public class SalaryCalculationServiceTest {

  private static final int HOURLY_RATE = 10_030;
  private static final Long WORKER_ID = 1L;

  @Mock
  private WorkplaceRepository workplaceRepository;

  @Mock
  private WorkerRepository workerRepository;

  @Mock
  private WorkRepository workRepository;

  @Mock
  private SalaryRepository salaryRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private SalaryCalculationService salaryCalculationService;

  // ---------------------------------------------------------------------
  // 테스트 6 — 야간 시간대(22:00~06:00) 경계 판정
  // ---------------------------------------------------------------------

  /// 야간 구간은 `[22:00, 24:00) ∪ [00:00, 06:00)`이다.
  ///
  /// 휴게시간을 0으로 두는 이유: C-5 수정(야간 분에서 휴게 제외)이 들어와도 이 기대값이
  /// 그대로 유지되도록 하기 위함이다. 휴게가 0이면 gross와 net이 같아 두 해석이 일치한다.
  @ParameterizedTest(name = "{4}: {0} ~ {1} → 야간 {3}분")
  @DisplayName("야간 근무시간 경계 판정")
  @CsvSource({
      // 시작,   종료,   종료가 익일인가, 기대 야간 분, 설명
      "21:00, 23:00, false, 60,  '뒤쪽 부분 겹침 - 22시 이후만 야간'",
      "05:00, 07:00, false, 60,  '앞쪽 부분 겹침 - 6시 이전만 야간'",
      "22:00, 22:01, false, 1,   '시작 경계 포함 - 22:00은 야간'",
      "06:00, 06:01, false, 0,   '종료 경계 제외 - 06:00은 주간'",
      "22:00, 06:00, true,  480, '야간 구간 전체'",
      "23:00, 02:00, true,  180, '자정 넘김'",
      "20:00, 08:00, true,  480, '두 야간 창을 모두 통과'",
      "09:00, 18:00, false, 0,   '주간 근무 - 야간 없음'",
  })
  void 야간_근무시간_경계_판정(String start, String end, boolean endsNextDay,
                       int expectedNightMinutes, String description) {
    LocalDate workDate = LocalDate.of(2025, 11, 10);
    LocalDateTime startTime = workDate.atTime(LocalTime.parse(start));
    LocalDateTime endTime = (endsNextDay ? workDate.plusDays(1) : workDate).atTime(LocalTime.parse(end));

    Work result = salaryCalculationService.calculateDailyIncome(
        work(1L, workDate, startTime, endTime, 0), 0, true);

    assertEquals(expectedNightMinutes, result.getNightWorkMinutes(), description);
  }

  @Test
  @DisplayName("야간 근무시간과 총 근무시간이 함께 계산된다")
  void 야간과_총_근무시간_동시_계산() {
    LocalDate workDate = LocalDate.of(2025, 11, 10);

    // 22:00 ~ 익일 06:00, 휴게 0분
    Work result = salaryCalculationService.calculateDailyIncome(
        work(1L, workDate,
            workDate.atTime(22, 0),
            workDate.plusDays(1).atTime(6, 0), 0), 0, true);

    assertAll(
        () -> assertEquals(480, result.getGrossWorkMinutes(), "총 근무시간"),
        () -> assertEquals(480, result.getNetWorkMinutes(), "순 근무시간 (휴게 0분)"),
        () -> assertEquals(480, result.getNightWorkMinutes(), "야간 근무시간"),
        // 8시간 × 10,030원
        () -> assertEquals(80_240, result.getBasePay(), "기본급"),
        // 8시간 × 10,030원 × 0.5
        () -> assertEquals(40_120, result.getNightAllowance(), "야간수당")
    );
  }

  // ---------------------------------------------------------------------
  // 테스트 7 — 재계산 멱등성
  // ---------------------------------------------------------------------

  /// 계산 결과를 다시 입력으로 넣어도 값이 변하지 않아야 한다.
  /// `calculateDailyIncome`이 `base_pay`/`gross_income` 같은 **저장된 결과**가 아니라
  /// `start_time`/`end_time`/`rest_time_minutes` **원본**에서 계산하기 때문에 성립한다.
  @Test
  @DisplayName("calculateDailyIncome은 자기 출력을 다시 입력해도 동일하다")
  void 일급_계산_멱등성() {
    LocalDate workDate = LocalDate.of(2025, 11, 10);
    Work input = work(1L, workDate,
        workDate.atTime(20, 0),
        workDate.plusDays(1).atTime(4, 0), 60);

    Work first = salaryCalculationService.calculateDailyIncome(input, 5_000, true);
    Work second = salaryCalculationService.calculateDailyIncome(first, 5_000, true);
    Work third = salaryCalculationService.calculateDailyIncome(second, 5_000, true);

    assertAll(
        () -> assertEquals(moneyOf(first), moneyOf(second), "1회차 → 2회차"),
        () -> assertEquals(moneyOf(second), moneyOf(third), "2회차 → 3회차")
    );
  }

  /// 주 단위 재계산을 DB 왕복까지 흉내내어 3회 연속 실행한다.
  /// 매 회차의 결과를 저장소 상태로 되먹여, 누적(accumulation) 로직이 섞여 들어오면 실패한다.
  @Test
  @DisplayName("recalculateWorkWeek을 3회 연속 실행해도 결과가 동일하다")
  void 주_단위_재계산_멱등성() {
    LocalDate monday = LocalDate.of(2025, 11, 10);
    AtomicReference<List<Work>> state = new AtomicReference<>(weekOfFiveWorks(monday));
    List<String> snapshots = new ArrayList<>();

    // 조회는 항상 현재 상태를 돌려준다 (주 조회 · 월 조회 모두)
    when(workRepository.findAllByWorkerIdAndDateRange(anyLong(), any(), any()))
        .thenAnswer(invocation -> new ArrayList<>(state.get()));

    // 배치 저장은 상태를 갱신하고 스냅샷을 남긴다 (= DB에 쓰인 값)
    doAnswer(invocation -> {
      List<Work> written = invocation.getArgument(0);
      state.set(new ArrayList<>(written));
      snapshots.add(written.stream().map(SalaryCalculationServiceTest::moneyOf)
          .collect(Collectors.joining(" | ")));
      return null;
    }).when(workRepository).updateWorkWeekDetailsBatch(any());

    for (int round = 0; round < 3; round++) {
      salaryCalculationService.recalculateWorkWeek(WORKER_ID, monday, hourlySalary());
    }

    assertEquals(3, snapshots.size(), "3회 모두 배치 저장이 일어나야 한다");
    assertAll(
        () -> assertEquals(snapshots.get(0), snapshots.get(1), "1회차 → 2회차"),
        () -> assertEquals(snapshots.get(1), snapshots.get(2), "2회차 → 3회차")
    );
  }

  // ---------------------------------------------------------------------
  // 헬퍼
  // ---------------------------------------------------------------------

  /// 금액·시간 필드만 뽑은 비교용 문자열. 어떤 필드가 어긋났는지 실패 메시지에 그대로 드러난다.
  private static String moneyOf(Work work) {
    return String.format("id=%d gross=%d net=%d night=%d base=%d nightAllow=%d holiday=%d total=%d",
        work.getId(), work.getGrossWorkMinutes(), work.getNetWorkMinutes(), work.getNightWorkMinutes(),
        work.getBasePay(), work.getNightAllowance(), work.getHolidayAllowance(), work.getGrossIncome());
  }

  private static Work work(Long id, LocalDate workDate,
                           LocalDateTime startTime, LocalDateTime endTime, int restTimeMinutes) {
    return Work.builder()
        .id(id)
        .workerId(WORKER_ID)
        .workDate(workDate)
        .startTime(startTime)
        .endTime(endTime)
        .restTimeMinutes(restTimeMinutes)
        .hourlyRate(HOURLY_RATE)
        .build();
  }

  /// 월~금 09:00–18:00, 휴게 60분 (순 근무 8시간 × 5일 = 주 40시간)
  private static List<Work> weekOfFiveWorks(LocalDate monday) {
    List<Work> works = new ArrayList<>();
    for (int day = 0; day < 5; day++) {
      LocalDate date = monday.plusDays(day);
      works.add(work((long) (day + 1), date, date.atTime(9, 0), date.atTime(18, 0), 60));
    }
    return works;
  }

  private static Salary hourlySalary() {
    return Salary.builder()
        .id(1L)
        .workerId(WORKER_ID)
        .salaryType(SalaryType.SALARY_MONTHLY)
        .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
        .hourlyRate(HOURLY_RATE)
        .salaryDate(25)
        .hasNationalPension(false)
        .hasHealthInsurance(false)
        .hasEmploymentInsurance(false)
        .hasIndustrialAccident(false)
        .hasIncomeTax(false)
        .hasHolidayAllowance(true)
        .hasNightAllowance(true)
        .build();
  }
}
