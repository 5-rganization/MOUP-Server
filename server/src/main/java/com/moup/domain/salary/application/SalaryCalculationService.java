package com.moup.domain.salary.application;

import com.moup.domain.salary.mapper.PublicHolidayRepository;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.user.dto.OwnerMonthlyWorkerSummaryResponse;
import com.moup.domain.user.dto.OwnerMonthlyWorkplaceSummaryResponse;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.WorkerHomeWorkplaceSummaryInfo;
import com.moup.domain.user.dto.WorkerMonthlyWorkplaceSummaryResponse;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.domain.workplace.dto.WorkplaceSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryCalculationService {

    private record DeductionDetails(
            int nationalPension,
            int healthInsurance,
            int employmentInsurance,
            int incomeTax,
            int localIncomeTax,
            int totalDeductions,
            int netIncome
    ) {}

    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final UserRepository userRepository;
    private final PublicHolidayRepository publicHolidayRepository;

    @Value("${salary.rates.national-pension}")
    private double nationalPensionRate;

    @Value("${salary.rates.health-insurance}")
    private double healthInsuranceRate;

    @Value("${salary.rates.long-term-care-insurance}")
    private double longTermCareInsuranceRate;

    @Value("${salary.rates.employment-insurance}")
    private double employmentInsuranceRate;

    @Value("${salary.rates.simple-income-tax}")
    private double incomeTaxRate;

    @Value("${salary.thresholds.insurance-min-hours}")
    private int insuranceMinHours;

    private static final LocalTime NIGHT_START_TIME = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END_TIME = LocalTime.of(6, 0);

    /// 특정 날짜가 포함된 '주' 단위로 급여(주휴수당 등)를 재계산합니다.
    @Transactional
    public void recalculateWorkWeek(Long workerId, LocalDate date, Salary salary) {
        if (salary != null && salary.getSalaryCalculation() == SalaryCalculation.SALARY_CALCULATION_FIXED) {
            return;
        }

        // 수당 적용 여부는 **근무 행의 스냅샷**을 쓴다. `salaries`의 현재값을 읽으면
        // 나중에 수당을 켰을 때 그 주의 근무 하나만 수정해도 그 주 전체가 새 정책으로
        // 재계산되어, 같은 알바생의 근무가 주마다 다른 정책으로 계산된다.
        // 어느 주가 어느 정책인지가 "수정을 건드렸는지"라는 무관한 이력에 좌우된다.
        // 확정 정책 3(급여 스냅샷).

        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Work> weekWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startOfWeek, endOfWeek);

        // 분자와 분모를 **같은 집합**에서 뽑는다. 예전에는 주 근무시간은 퇴근 기록이 있는 근무만
        // 합산하면서 배분 분모는 전체 건수를 써, 미퇴근 근무가 섞이면 주휴수당이 그만큼 증발했다.
        List<Work> payableWorks = weekWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .toList();

        long weeklyWorkMinutes = payableWorks.stream()
                .mapToLong(SalaryCalculationService::netMinutesOf)
                .sum();

        // 확정 정책 4의 기준 시급과 같은 규칙 — 그 주 **마지막 근무**의 설정을 따른다.
        boolean hasHolidayAllowance = !payableWorks.isEmpty()
                && Boolean.TRUE.equals(payableWorks.get(payableWorks.size() - 1).getHasHolidayAllowance());

        // 개근 요건 — 그 주에 결근이 하나라도 있으면 주휴수당이 없다 (근로기준법 시행령 제30조 제1항).
        // 15시간 판정(weeklyWorkMinutes)은 결근한 날의 예정 시간도 포함한다. 기준이 실근로시간이 아니라
        // 소정근로시간이기 때문이다. 결근이 있으면 어차피 0이라 지금은 결과에 영향이 없다.
        boolean hasAbsence = weekWorks.stream().anyMatch(SalaryCalculationService::isAbsent);
        int weeklyHolidayAllowance = hasAbsence ? 0 : calculateWeeklyHolidayAllowance(
                payableWorks, weeklyWorkMinutes, hasHolidayAllowance);

        // 주휴수당을 근무일에 배분한다. 정수 나눗셈 나머지는 버리지 않고 마지막 근무일에 몰아준다
        // (주당 최대 근무일수-1원이 사라지던 문제).
        int perWorkAllowance = payableWorks.isEmpty() ? 0 : weeklyHolidayAllowance / payableWorks.size();
        int allowanceRemainder = payableWorks.isEmpty() ? 0 : weeklyHolidayAllowance % payableWorks.size();

        // 연장(1일 8시간·주 40시간 초과)과 공휴일 근무시간은 주 전체를 봐야 정해진다. 수당은 근무 행의 스냅샷을 따른다.
        // 일요일 밤에 시작해 다음 주 월요일(공휴일)로 넘어가는 근무가 있으므로 하루 더 읽는다.
        Set<LocalDate> publicHolidays = Set.copyOf(
                publicHolidayRepository.findDatesBetween(startOfWeek, endOfWeek.plusDays(1)));
        warnIfPublicHolidaysMissing(startOfWeek.getYear());
        warnIfPublicHolidaysMissing(endOfWeek.getYear());
        Map<Work, PremiumMinutes> premiumByWork = allocatePremiumMinutes(weekWorks, publicHolidays);

        List<Work> updatedWorks = new ArrayList<>(weekWorks.size());
        for (Work work : weekWorks) {
            int index = payableWorks.indexOf(work);
            int dailyHolidayAllowance = 0;
            if (index >= 0) {
                dailyHolidayAllowance = perWorkAllowance
                        + (index == payableWorks.size() - 1 ? allowanceRemainder : 0);
            }
            // 퇴근 기록이 없는 근무도 통과시킨다. calculateDailyIncome이 0으로 초기화하므로
            // 예전 계산 결과가 stale하게 남지 않는다.
            updatedWorks.add(calculateDailyIncome(work, dailyHolidayAllowance,
                    Boolean.TRUE.equals(work.getHasNightAllowance()),
                    premiumByWork.getOrDefault(work, PremiumMinutes.NONE)));
        }

        // 해당 주의 모든 근무일에 대해 일급을 재계산합니다.
        if (!updatedWorks.isEmpty()) {
            workRepository.updateWorkWeekDetailsBatch(updatedWorks);
        }

        // 마지막으로, 월 전체의 '추정 세후 일급'을 다시 계산하여 캘린더 표시용 데이터를 업데이트합니다.
        recalculateEstimatedNetIncomeForMonth(workerId, date.getYear(), date.getMonthValue(), salary);
    }

    /// 연장근로 기준 (1일 8시간). 휴일근로 가산이 50%에서 100%로 바뀌는 기준도 같다.
    private static final long DAILY_REGULAR_MINUTES = 8 * 60L;

    /// 한 근무에 붙는 가산 시간. 금액은 근무 행의 수당 스냅샷을 보고 `calculateDailyIncome`이 정한다.
    ///
    /// @param overtime              연장 근무시간(분)
    /// @param publicHoliday         공휴일 근무시간(분)
    /// @param publicHolidayOverEight 그중 그날 8시간을 넘는 분 — 50%가 아니라 100%가 붙는다
    public record PremiumMinutes(long overtime, long publicHoliday, long publicHolidayOverEight) {
        public static final PremiumMinutes NONE = new PremiumMinutes(0, 0, 0);
    }

    /// 주 단위 가산 시간 배분.
    ///
    /// **연장(Phase 11 D11-2)** — 근로기준법 제50·56조, 1일 8시간 또는 1주 40시간을 넘는 시간.
    /// 1. 날마다 8시간을 넘는 분을 그날 **뒤쪽 근무**부터 연장으로 뗀다(같은 날 근무가 둘이면 나중 근무).
    /// 2. 그러고 남은 시간의 주 합계가 40시간을 넘으면, 그 초과분을 주의 **뒤쪽 근무**부터 연장으로 뗀다.
    ///    40시간을 넘기는 것은 시간상 마지막 근무들이다. 1에서 뗀 분은 다시 세지 않는다(이중 가산 방지).
    ///
    /// **휴일(Phase 11 Q6)** — 같은 조 제2항, 휴일근로는 8시간까지 50%, 넘는 분은 100%.
    /// 공휴일 근무시간은 연장 판정에서 뺀다. 휴일근로 가산이 그 시간의 가산이고, 연장으로 또 세면 같은 분에 두 번 붙는다.
    /// 8시간 기준은 근무일(`workDate`) 단위로 본다 — 공휴일 이틀에 걸친 밤샘 근무는 시작한 날로 묶인다.
    ///
    /// 결근·퇴근 기록 없는 근무는 일하지 않았으므로 세지 않는다. 고정급은 `recalculateWorkWeek`이 조기 반환해 여기까지 오지 않는다.
    private static Map<Work, PremiumMinutes> allocatePremiumMinutes(List<Work> weekWorks, Set<LocalDate> publicHolidays) {
        List<Work> worked = weekWorks.stream()
                .filter(work -> work.getEndTime() != null && !isAbsent(work))
                .sorted(Comparator.comparing(Work::getStartTime))
                .toList();
        Map<Work, Long> regular = new IdentityHashMap<>();
        Map<Work, Long> holiday = new IdentityHashMap<>();
        Map<Work, Long> overtime = new IdentityHashMap<>();
        Map<Work, Long> holidayOverEight = new IdentityHashMap<>();
        for (Work work : worked) {
            long net = netMinutesOf(work);
            long holidayMinutes = publicHolidayNetMinutes(work, net, publicHolidays);
            regular.put(work, net - holidayMinutes);
            holiday.put(work, holidayMinutes);
            overtime.put(work, 0L);
            holidayOverEight.put(work, 0L);
        }

        Map<LocalDate, List<Work>> byDay = worked.stream()
                .collect(Collectors.groupingBy(Work::getWorkDate, LinkedHashMap::new, Collectors.toList()));
        for (List<Work> day : byDay.values()) {
            assignFromLatest(day, day.stream().mapToLong(regular::get).sum() - DAILY_REGULAR_MINUTES, regular, overtime);
            assignFromLatest(day, day.stream().mapToLong(holiday::get).sum() - DAILY_REGULAR_MINUTES, holiday, holidayOverEight);
        }
        long weeklyRegular = worked.stream().mapToLong(work -> regular.get(work) - overtime.get(work)).sum();
        assignFromLatest(worked, weeklyRegular - FULL_TIME_WEEKLY_MINUTES, regular, overtime);

        Map<Work, PremiumMinutes> result = new IdentityHashMap<>();
        for (Work work : worked) {
            result.put(work, new PremiumMinutes(overtime.get(work), holiday.get(work), holidayOverEight.get(work)));
        }
        return result;
    }

    /// `excessMinutes`를 뒤쪽 근무부터 `assigned`에 배정한다. 근무마다 `capacity - assigned`까지만 받는다.
    private static void assignFromLatest(List<Work> works, long excessMinutes,
                                         Map<Work, Long> capacity, Map<Work, Long> assigned) {
        for (int i = works.size() - 1; i >= 0 && excessMinutes > 0; i--) {
            Work work = works.get(i);
            long take = Math.min(capacity.get(work) - assigned.get(work), excessMinutes);
            assigned.merge(work, take, Long::sum);
            excessMinutes -= take;
        }
    }

    /// 근무 중 공휴일(0시~24시)에 시작하는 분의 수를 휴게시간 비례로 줄인 값. 야간 시간과 같은 원칙이다.
    private static long publicHolidayNetMinutes(Work work, long netMinutes, Set<LocalDate> publicHolidays) {
        if (publicHolidays.isEmpty()) {
            return 0;
        }
        LocalDateTime start = work.getStartTime();
        long gross = countStartedMinutes(start, work.getEndTime());
        if (gross == 0) {
            return 0;
        }
        long holidayGross = 0;
        for (LocalDate day = start.toLocalDate(); !day.isAfter(work.getEndTime().toLocalDate()); day = day.plusDays(1)) {
            if (publicHolidays.contains(day)) {
                long first = Math.max(0, minuteIndexAtOrAfter(start, day.atStartOfDay()));
                long last = Math.min(gross, minuteIndexAtOrAfter(start, day.plusDays(1).atStartOfDay()));
                holidayGross += Math.max(0, last - first);
            }
        }
        return Math.min(netMinutes, holidayGross * netMinutes / gross);
    }

    /// `start + k분`이 `instant` 이상이 되는 가장 작은 k. 음수일 수 있다.
    private static long minuteIndexAtOrAfter(LocalDateTime start, LocalDateTime instant) {
        long nanos = Duration.between(start, instant).toNanos();
        return -Math.floorDiv(-nanos, 60_000_000_000L);
    }

    /// 공휴일 표는 매년 갱신해야 한다. 갱신하지 않은 해의 근무에는 휴일 가산이 조용히 0이 되므로 한 번은 알린다.
    private final Set<Integer> checkedPublicHolidayYears = ConcurrentHashMap.newKeySet();

    private void warnIfPublicHolidaysMissing(int year) {
        if (checkedPublicHolidayYears.add(year) && !publicHolidayRepository.existsInYear(year)) {
            log.warn("{}년 공휴일이 public_holidays에 없습니다. 그해 근무에는 휴일근로 가산이 붙지 않습니다.", year);
        }
    }

    /// 주휴수당 발생 하한 (주 15시간).
    private static final long MIN_WEEKLY_MINUTES_FOR_HOLIDAY_ALLOWANCE = 15 * 60L;
    /// 통상 근로자의 주 소정근로시간 (40시간).
    private static final long FULL_TIME_WEEKLY_MINUTES = 40 * 60L;
    /// 주휴수당 상한 일수 (1일 8시간).
    private static final long HOLIDAY_ALLOWANCE_MINUTES = 8 * 60L;

    private static boolean isAbsent(Work work) {
        return Boolean.TRUE.equals(work.getIsAbsent());
    }

    /// 휴게시간을 뺀 순 근무시간. 휴게가 근무보다 길어도 음수가 되지 않게 막는다.
    /// 예전에는 주간 합계에만 클램프가 없어 개별 근무의 `net_work_minutes` 합과
    /// 주간 합계가 서로 달랐고, 15시간·40시간 임계 근처에서 주휴수당 발생 여부가 뒤집혔다.
    private static long netMinutesOf(Work work) {
        long gross = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();
        long rest = work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0L;
        return Math.max(0L, gross - rest);
    }

    /// 주휴수당 = `min(주 소정근로시간 ÷ 40, 1.0) × 8 × 시급` (주 15시간 이상일 때만).
    ///
    /// 근로기준법 시행령 제9조 제1항 별표2. 예전 산식은
    /// `(주 총 근로시간 ÷ 근무일수) × 시급`으로 **1일 평균 근무시간에 시급을 곱했고
    /// 8시간 상한도 없었다.** 주 5일 근무일 때만 우연히 일치했고,
    /// 주 2일(10시간씩) 근무자에게는 2.5배를 지급했다.
    private int calculateWeeklyHolidayAllowance(List<Work> payableWorks, long weeklyWorkMinutes,
                                                boolean hasHolidayAllowance) {
        if (!hasHolidayAllowance || payableWorks.isEmpty()
                || weeklyWorkMinutes < MIN_WEEKLY_MINUTES_FOR_HOLIDAY_ALLOWANCE) {
            return 0;
        }
        Integer baseHourlyRate = resolveWeeklyBaseHourlyRate(payableWorks);
        if (baseHourlyRate == null) {
            log.warn("주휴수당 산정 불가 - 그 주 근무에 시급 정보가 없습니다. workDate={}",
                    payableWorks.get(0).getWorkDate());
            return 0;
        }
        long cappedMinutes = Math.min(weeklyWorkMinutes, FULL_TIME_WEEKLY_MINUTES);
        return roundHalfUp(cappedMinutes * HOLIDAY_ALLOWANCE_MINUTES * baseHourlyRate,
                FULL_TIME_WEEKLY_MINUTES * 60L);
    }

    /// 확정 정책 4 — 기준 시급은 **그 주 마지막 근무**의 시급이다.
    ///
    /// `works.hourly_rate`는 NULL을 허용하므로(레거시 행) 뒤에서부터 첫 non-null을 찾는다.
    /// 예전에는 `weekWorks.get(0).getHourlyRate()`를 그대로 언박싱해
    /// **NULL 한 건이 그 주 전체를 500으로 만들었다.**
    private Integer resolveWeeklyBaseHourlyRate(List<Work> payableWorks) {
        for (int i = payableWorks.size() - 1; i >= 0; i--) {
            Integer rate = payableWorks.get(i).getHourlyRate();
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /// 하루 근무에 대한 세전 일급(각종 수당 포함)을 상세하게 계산합니다. 연장 근무시간은 주를 봐야 정해지므로
    /// 0으로 둔다 — 근무를 만들거나 고친 뒤에는 항상 `recalculateWorkWeek`이 이어서 채운다.
    public Work calculateDailyIncome(Work work, int dailyHolidayAllowance, boolean hasNightAllowance) {
        return calculateDailyIncome(work, dailyHolidayAllowance, hasNightAllowance, PremiumMinutes.NONE);
    }

    /// `premium`은 `allocatePremiumMinutes`가 정한 이 근무의 가산 시간이다. 연장·휴일 수당을 붙일지는 근무 행의 스냅샷을 본다.
    public Work calculateDailyIncome(Work work, int dailyHolidayAllowance, boolean hasNightAllowance,
                                     PremiumMinutes premium) {
        // end_time이 없으면 (아직 근무 중) 급여를 0으로 계산하고 반환.
        // 결근도 같다 — 일하지 않았으므로 시간도 급여도 0이다. 예정 시간은 행에 그대로 남는다.
        if (work.getEndTime() == null || isAbsent(work)) {
            return work.toBuilder()
                    .grossWorkMinutes(0)
                    .netWorkMinutes(0)
                    .nightWorkMinutes(0)
                    .overtimeWorkMinutes(0)
                    .publicHolidayWorkMinutes(0)
                    .basePay(0)
                    .nightAllowance(0)
                    .holidayAllowance(0)
                    .overtimeAllowance(0)
                    .publicHolidayAllowance(0)
                    .grossIncome(0)
                    .build();
        }

        LocalDateTime start = work.getStartTime();
        LocalDateTime end = work.getEndTime();
        int restMinutes = work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0;

        // --- 야간 근무 시간 계산 ---
        //
        // 야간 시간은 **사실 기록**이므로 야간수당 설정과 무관하게 항상 센다.
        // 예전에는 `if (hasNightAllowance)` 안에서만 세어, 수당을 끄고 일한 기간은
        // night_work_minutes가 0으로 남았다. 나중에 수당을 켜도 그 기간은 복원되지 않는다.
        long grossWorkMinutes = countStartedMinutes(start, end);
        long nightWorkMinutes = countNightMinutes(start, grossWorkMinutes);

        long netWorkMinutes = Math.max(0L, grossWorkMinutes - restMinutes);

        // 휴게시간이 언제 소진됐는지는 기록에 없다. 근무 시간대에 비례해 배분한다
        // (확정 정책 1의 "근무 시간대로 배분"과 같은 원칙).
        //
        // 이 보정이 없으면 net(휴게 제외)에서 야간(휴게 미제외)을 빼는 곳에서
        // **주간 근무시간이 음수**가 된다.
        long netNightMinutes = (grossWorkMinutes == 0)
                ? 0
                : nightWorkMinutes * netWorkMinutes / grossWorkMinutes;

        // --- 수당 계산 ---
        // 정수 연산으로 계산한다. `분 / 60.0 * 시급`은 이진 부동소수점 오차 때문에
        // 값이 1원씩 낮게 떨어졌다(시급 12,000 × 246분 → 49,199원, 정답 49,200원).
        // 시급 1,000~30,000 × 1~1,440분 전수 탐색에서 90,310건이 어긋났고,
        // 절삭 방향은 **항상 근로자에게 불리**했다.
        int hourlyRate = (work.getHourlyRate() != null) ? work.getHourlyRate() : 0;
        // 지급 항목은 원 단위 반올림이다 (스코프 3 Q7). 예전에는 정수 나눗셈이라 항상 버림이었다.
        int basePay = roundHalfUp(netWorkMinutes * hourlyRate, 60L);

        int nightAllowance = 0;
        if (hasNightAllowance) {
            // 야간 가산 50% → 분 × 시급 × 0.5 / 60 = 분 × 시급 / 120
            nightAllowance = roundHalfUp(netNightMinutes * hourlyRate, 120L);
        }

        // 연장 가산 50% → 분 × 시급 / 120. 야간과 겹치면 둘 다 붙는다(가산 사유가 다르다).
        long overtimeWorkMinutes = Math.min(Math.max(0L, premium.overtime()), netWorkMinutes);
        int overtimeAllowance = Boolean.TRUE.equals(work.getHasOvertimeAllowance())
                ? roundHalfUp(overtimeWorkMinutes * hourlyRate, 120L) : 0;

        // 휴일 가산 — 8시간까지 50%, 넘는 분은 100%(50%를 한 번 더). 야간과 겹치면 둘 다 붙는다.
        long publicHolidayWorkMinutes = Math.min(Math.max(0L, premium.publicHoliday()), netWorkMinutes);
        long publicHolidayOverEight = Math.min(Math.max(0L, premium.publicHolidayOverEight()), publicHolidayWorkMinutes);
        int publicHolidayAllowance = Boolean.TRUE.equals(work.getHasPublicHolidayAllowance())
                ? roundHalfUp(publicHolidayWorkMinutes * hourlyRate, 120L) + roundHalfUp(publicHolidayOverEight * hourlyRate, 120L)
                : 0;

        int grossIncome = basePay + nightAllowance + dailyHolidayAllowance + overtimeAllowance + publicHolidayAllowance;

        // 계산된 모든 급여 항목을 Work 객체로 반환합니다.
        return work.toBuilder()
                .grossWorkMinutes((int) grossWorkMinutes)
                .netWorkMinutes((int) netWorkMinutes)
                .nightWorkMinutes((int) netNightMinutes)
                .overtimeWorkMinutes((int) overtimeWorkMinutes)
                .publicHolidayWorkMinutes((int) publicHolidayWorkMinutes)
                .basePay(basePay)
                .nightAllowance(nightAllowance)
                .holidayAllowance(dailyHolidayAllowance)
                .overtimeAllowance(overtimeAllowance)
                .publicHolidayAllowance(publicHolidayAllowance)
                .grossIncome(grossIncome)
                .hourlyRate(work.getHourlyRate())
                .build();
    }

    private static final long MINUTES_PER_DAY = 24 * 60L;
    /// 야간(22:00~06:00)을 22:00 기준으로 당기면 하루의 앞 8시간(0~480분)이 된다.
    private static final long NIGHT_SHIFT_MINUTES = MINUTES_PER_DAY - NIGHT_START_TIME.toSecondOfDay() / 60;
    private static final long NIGHT_LENGTH_MINUTES = NIGHT_SHIFT_MINUTES + NIGHT_END_TIME.toSecondOfDay() / 60;

    /// `start`부터 1분씩 나아가며 `end` 전에 시작하는 분의 개수 (= 근무 분, 초가 있으면 올림).
    ///
    /// 예전에는 이것과 야간 분을 1분짜리 루프로 셌다. 근무 시간에 상한이 없어 10년짜리 근무 하나로
    /// 약 526만 번을 돌았다(스코프 2 I7 · 3 I-10). 같은 값을 상수 시간에 낸다.
    private static long countStartedMinutes(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            return 0;
        }
        Duration duration = Duration.between(start, end);
        long whole = duration.toMinutes();
        return duration.equals(Duration.ofMinutes(whole)) ? whole : whole + 1;
    }

    /// `start`부터 시작하는 `minutes`개의 분 중 시작 시각이 야간(22:00 이상 또는 06:00 미만)인 분의 개수.
    ///
    /// 각 분이 야간인지는 그 분이 시작하는 **시·분**만으로 정해진다(초는 무관). 시작 분을 22:00 기준으로
    /// 당기면 야간은 매일 [0, 480)분 구간이 되므로, 하루 단위 반복과 남은 구간의 겹침으로 센다.
    private static long countNightMinutes(LocalDateTime start, long minutes) {
        long startMinuteOfDay = start.toLocalTime().toSecondOfDay() / 60;
        long offset = (startMinuteOfDay + NIGHT_SHIFT_MINUTES) % MINUTES_PER_DAY;
        long fullDays = minutes / MINUTES_PER_DAY;
        long rest = minutes % MINUTES_PER_DAY;
        // 남은 구간 [offset, offset + rest)는 하루를 넘지 않으므로 오늘·내일 야간 두 구간과의 겹침만 보면 된다.
        long tail = overlap(offset, offset + rest, 0, NIGHT_LENGTH_MINUTES)
                + overlap(offset, offset + rest, MINUTES_PER_DAY, MINUTES_PER_DAY + NIGHT_LENGTH_MINUTES);
        return fullDays * NIGHT_LENGTH_MINUTES + tail;
    }

    private static long overlap(long from, long to, long rangeFrom, long rangeTo) {
        return Math.max(0, Math.min(to, rangeTo) - Math.max(from, rangeFrom));
    }

    /// 캘린더에 표시될 '추정 세후 일급'을 월 단위로 재계산합니다.
    @Transactional
    public void recalculateEstimatedNetIncomeForMonth(Long workerId, int year, int month, Salary salaryInfo) {
        // 1. 서울 기준의 '시작일'과 '종료일'의 범위(ZonedDateTime)를 정확히 계산
        LocalDate rawStartDate = LocalDate.of(year, month, 1);

        // 서울 기준 1일 00:00:00
        ZonedDateTime startZoned = rawStartDate.atStartOfDay(SEOUL_ZONE_ID);

        // 서울 기준 말일의 마지막 순간 (나노초까지 포함하여 해당 월을 꽉 채움)
        ZonedDateTime endZoned = rawStartDate.with(TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX)
                .atZone(SEOUL_ZONE_ID);

        // 2. 비즈니스 로직(주급 계산 등)을 위한 LocalDate 추출 (서울 시간 기준의 날짜)
        LocalDate startDate = startZoned.toLocalDate();
        LocalDate endDate = endZoned.toLocalDate();

        // ---------------------------------------------------------
        // Repository 호출: 가능한 정확한 Instant(UTC) 범위를 넘겨줍니다.
        // (Repository 파라미터가 LocalDate라면 startZoned.toLocalDate()를 넣어야 하지만,
        //  정확한 조회를 위해선 아래처럼 Instant로 변환된 값을 넘기는 게 베스트입니다.)
        List<Work> monthWorks = workRepository.findAllByWorkerIdAndDateRange(
                workerId,
                startZoned.toLocalDate(), // 서울 00시 -> UTC 변환값
                endZoned.toLocalDate()    // 서울 23시 -> UTC 변환값
        );
        // ---------------------------------------------------------

        // 현재까지의 근무 기록을 바탕으로 예상 월급을 추정합니다.
        int currentGrossSum = monthWorks.stream()
                .mapToInt(work -> work.getGrossIncome() != null ? work.getGrossIncome() : 0)
                .sum();
        // 결근한 날은 근무일이 아니다. 세면 공제액이 0원짜리 날에도 배분되어 하루치가 사라지고,
        // 일급제 고정급이 결근일까지 지급된다.
        long daysWorked = monthWorks.stream().filter(work -> !isAbsent(work)).count();

        int estimatedMonthlyIncome = 0;

        if (salaryInfo != null && salaryInfo.getSalaryCalculation() == SalaryCalculation.SALARY_CALCULATION_FIXED) {
            // --- 고정급제 ---
            if (daysWorked != 0) {
                int fixedRate = (salaryInfo.getFixedRate() != null) ? salaryInfo.getFixedRate() : 0;
                switch (salaryInfo.getSalaryType()) {
                    case SALARY_MONTHLY:
                        estimatedMonthlyIncome = fixedRate;
                        break;
                    case SALARY_WEEKLY:
                        DayOfWeek payDayOfWeek = salaryInfo.getSalaryDay();
                        int payDayCount = 0;
                        if (payDayOfWeek != null) {
                            // 주급 계산 로직 (서울 기준 날짜로 반복)
                            LocalDate dateIterator = startDate;
                            while (!dateIterator.isAfter(endDate)) {
                                if (dateIterator.getDayOfWeek() == payDayOfWeek) {
                                    payDayCount++;
                                }
                                dateIterator = dateIterator.plusDays(1);
                            }
                        }
                        estimatedMonthlyIncome = fixedRate * payDayCount;
                        break;
                    case SALARY_DAILY:
                        estimatedMonthlyIncome = fixedRate * (int) daysWorked;
                        break;
                }
            }
        } else {
            // --- 시급제 ---
            estimatedMonthlyIncome = currentGrossSum;
        }

        long totalMinutesWorked = monthWorks.stream()
                .filter(work -> work.getEndTime() != null && !isAbsent(work))
                .mapToLong(SalaryCalculationService::netMinutesOf)   // 음수 클램프 포함
                .sum();

        long estimatedTotalHours = 0;
        if (daysWorked > 0) {
            estimatedTotalHours = totalMinutesWorked / 60;
        }

        // 근무일 0일 처리
        if (daysWorked == 0) {
            // 업데이트 쿼리에도 정확한 Instant 범위 전달
            workRepository.updateEstimatedNetIncomeToZeroByDateRange(
                    workerId,
                    startZoned.toLocalDate(),
                    endZoned.toLocalDate()
            );
            return;
        }

        int estimatedMonthlyDeduction = 0;
        if (salaryInfo != null) {
            DeductionDetails deductions = calculateDeductions(estimatedMonthlyIncome, estimatedTotalHours, salaryInfo);
            estimatedMonthlyDeduction = deductions.totalDeductions();
        }

        int estimatedDailyDeduction = (int) (estimatedMonthlyDeduction / (double) daysWorked);

        // 최종 업데이트: 여기도 정확한 Instant 범위 사용
        workRepository.updateAllEstimatedNetIncomesForMonth(
                workerId,
                startZoned.toLocalDate(),
                endZoned.toLocalDate(),
                estimatedDailyDeduction
        );
    }

    private static int countAttendedDays(List<Work> works) {
        return (int) works.stream().filter(work -> !isAbsent(work)).count();
    }

    /// 보험 적용 대상인지 판단하는 헬퍼 메서드
    private boolean isInsuranceApplicable(int monthlyIncome, long monthlyHours) {
        // 월 소득 220만원 이상 또는 월 60시간 이상 근무 시 (조건은 정책에 따라 변경 가능)
        // 실제로는 더 복잡한 조건(고용 기간 등)이 있으나 요청에 따라 간소화
        return monthlyIncome >= 2_200_000 || monthlyHours >= insuranceMinHours;
    }

    /// 알바생이 특정 월에 근무지별로 받은 급여 상세 내역(시간, 수당, 공제액)을 조회합니다. (알바생 전용)
    @Transactional(readOnly = true)
    public List<WorkerMonthlyWorkplaceSummaryResponse> getWorkerMonthlyWorkplaceSummaryList(Long userId, int year, int month) {

        // 1. 사용자가 속한 모든 'Worker' 목록을 가져옵니다 (근무지 목록)
        List<Worker> userWorkerList = workerRepository.findAllByUserId(userId);
        if (userWorkerList.isEmpty()) { return Collections.emptyList(); }

        List<WorkerMonthlyWorkplaceSummaryResponse> summaryResponseList = new ArrayList<>();
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        // 2. N+1 방지를 위해 필요한 정보를 미리 조회합니다.
        List<Long> workerIdList = userWorkerList.stream().map(Worker::getId).toList();

        // [쿼리 1] 모든 Salary 정보
        Map<Long, Salary> salaryMap = salaryRepository.findAllByWorkerIdListIn(workerIdList)
                .stream()
                .collect(Collectors.toMap(Salary::getWorkerId, s -> s));

        // [쿼리 2] 모든 Work 정보
        Map<Long, List<Work>> workMap = workRepository.findAllByWorkerIdListInAndDateRange(workerIdList, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // [쿼리 3] 모든 Workplace 정보
        List<Long> workplaceIdList = userWorkerList.stream().map(Worker::getWorkplaceId).distinct().toList();
        Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdListIn(workplaceIdList)
                .stream()
                .collect(Collectors.toMap(Workplace::getId, w -> w));


        // 3. 각 근무지(Worker)별로 순회하며 DTO를 조립합니다.
        for (Worker worker : userWorkerList) {
            Long workerId = worker.getId();

            // [필터 1] Salary 정보가 없는 근무자(사장님)는 건너뜁니다.
            Salary salaryInfo = salaryMap.get(workerId);
            if (salaryInfo == null) { continue; }

            // [필터 2] 해당 월에 근무 기록
            List<Work> workList = workMap.getOrDefault(workerId, Collections.emptyList());

            // [필터 3] 근무지 정보 조회
            Workplace workplace = workplaceMap.get(worker.getWorkplaceId());
            WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            WorkerHomeWorkplaceSummaryInfo workerHomeWorkplaceSummaryInfo = WorkerHomeWorkplaceSummaryInfo.builder()
                    .workplaceSummaryInfo(workplaceSummaryInfo)
                    .isNowWorking(worker.getIsNowWorking())
                    .build();

            // 4. 시간 및 수당 계산 (DB에 저장된 값을 합산)
            // 이 값들은 DTO 표시용으로 '고정급' 여부와 관계없이 항상 계산합니다.
            long totalWorkMinutes = workList.stream() // 순 근무시간 합계
                    .mapToLong(work -> work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0)
                    .sum();

            long totalNightMinutes = workList.stream() // 야간 근무 시간(분) 합계
                    .mapToLong(work -> work.getNightWorkMinutes() != null ? work.getNightWorkMinutes() : 0)
                    .sum();

            long totalRestTimeMinutes = workList.stream() // 휴게 시간(분) 합계
                    .mapToLong(work -> work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0)
                    .sum();

            int totalHolidayAllowance = workList.stream() // 주휴수당(원) 합계
                    .mapToInt(work -> work.getHolidayAllowance() != null ? work.getHolidayAllowance() : 0)
                    .sum();

            int dayTimeIncome = workList.stream() // --- 총 주간 근무 급여 (기본급) 합계 ---
                    .mapToInt(work -> work.getBasePay() != null ? work.getBasePay() : 0)
                    .sum();

            int totalNightAllowance = workList.stream() // --- 야간수당(원) 합계 계산 ---
                    .mapToInt(work -> work.getNightAllowance() != null ? work.getNightAllowance() : 0)
                    .sum();

            int totalOvertimeAllowance = workList.stream()
                    .mapToInt(work -> work.getOvertimeAllowance() != null ? work.getOvertimeAllowance() : 0)
                    .sum();
            long totalOvertimeMinutes = workList.stream()
                    .mapToLong(work -> work.getOvertimeWorkMinutes() != null ? work.getOvertimeWorkMinutes() : 0)
                    .sum();
            int totalPublicHolidayAllowance = workList.stream()
                    .mapToInt(work -> work.getPublicHolidayAllowance() != null ? work.getPublicHolidayAllowance() : 0)
                    .sum();
            long totalPublicHolidayMinutes = workList.stream()
                    .mapToLong(work -> work.getPublicHolidayWorkMinutes() != null ? work.getPublicHolidayWorkMinutes() : 0)
                    .sum();


            // 4-1. 급여 계산 방식(SalaryCalculation)에 따른 세전 총 소득(grossIncome) 계산
            int grossIncome = 0;
            int fixedRate = (salaryInfo.getFixedRate() != null) ? salaryInfo.getFixedRate() : 0;

            if (salaryInfo.getSalaryCalculation() == SalaryCalculation.SALARY_CALCULATION_FIXED) {
                // --- 고정급제 ---
                // 고정급이라도 근무 기록(workList)이 없으면 0원
                if (workList.isEmpty()) {
                    grossIncome = 0;
                } else {
                    switch (salaryInfo.getSalaryType()) {
                        case SALARY_MONTHLY:
                            // 월급: 고정급(fixedRate)이 월급 총액
                            grossIncome = fixedRate;
                            break;
                        case SALARY_WEEKLY:
                            // 주급: 고정급(fixedRate) * 해당 월의 주급 지급 횟수
                            DayOfWeek payDayOfWeek = salaryInfo.getSalaryDay();
                            int payDayCount = 0;
                            if (payDayOfWeek != null) {
                                LocalDate dateIterator = startDate; // 해당 월의 1일
                                while (!dateIterator.isAfter(endDate)) { // 해당 월의 마지막 날까지
                                    if (dateIterator.getDayOfWeek() == payDayOfWeek) {
                                        payDayCount++;
                                    }
                                    dateIterator = dateIterator.plusDays(1);
                                }
                            }
                            grossIncome = fixedRate * payDayCount;
                            break;
                        case SALARY_DAILY:
                            // 일급: 고정급(fixedRate) * 해당 월의 근무일 수 (결근일 제외)
                            grossIncome = fixedRate * countAttendedDays(workList);
                            break;
                    }
                }
            } else {
                // --- 시급제 (SALARY_CALCULATION_HOURLY) ---
                // Work 레코드에 기록된 모든 세전 일급(grossIncome)을 합산
                grossIncome = workList.stream()
                        .mapToInt(work -> work.getGrossIncome() != null ? work.getGrossIncome() : 0)
                        .sum();
            }

            long totalWorkHours = totalWorkMinutes / 60;

            // --- 5. 공제액 계산 ---
            DeductionDetails deductions = calculateDeductions(grossIncome, totalWorkHours, salaryInfo);

            // --- 5-1. 급여일 D-day 계산 ---
            Integer daysUntilPayday = null;
            LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
            SalaryType salaryType = salaryInfo.getSalaryType();

            if (salaryType == SalaryType.SALARY_MONTHLY) {
                Integer payDayOfMonth = salaryInfo.getSalaryDate();
                if (payDayOfMonth != null) {

                    LocalDate thisMonthPayday;
                    try {
                        // 1. 이번 달의 급여일 날짜를 계산
                        thisMonthPayday = today.withDayOfMonth(payDayOfMonth);
                    } catch (java.time.DateTimeException e) {
                        // 2. (예: 2월 30일)처럼 유효하지 않으면, 이번 달의 마지막 날로 설정
                        thisMonthPayday = today.with(TemporalAdjusters.lastDayOfMonth());
                    }

                    LocalDate nextPayday;
                    if (today.isAfter(thisMonthPayday)) {
                        // 3. 오늘이 이번 달 급여일보다 늦었다면, '다음 달'의 급여일을 계산
                        LocalDate nextMonth = today.plusMonths(1);
                        try {
                            // 4. 다음 달의 급여일 날짜를 계산
                            nextPayday = nextMonth.withDayOfMonth(payDayOfMonth);
                        } catch (java.time.DateTimeException e) {
                            // 5. (예: 4월 31일)처럼 유효하지 않으면, 다음 달의 마지막 날로 설정
                            nextPayday = nextMonth.with(TemporalAdjusters.lastDayOfMonth());
                        }
                    } else {
                        // 6. 아직 이번 달 급여일이 지나지 않았으면, 이번 달 급여일이 D-day 대상
                        nextPayday = thisMonthPayday;
                    }

                    daysUntilPayday = (int) ChronoUnit.DAYS.between(today, nextPayday);
                }
            } else if (salaryType == SalaryType.SALARY_WEEKLY) {
                DayOfWeek payDayOfWeek = salaryInfo.getSalaryDay();
                if (payDayOfWeek != null) {
                    // 오늘을 포함하여 다음 번 돌아오는 급여 요일
                    LocalDate nextPayday = today.with(TemporalAdjusters.nextOrSame(payDayOfWeek));
                    daysUntilPayday = (int) ChronoUnit.DAYS.between(today, nextPayday);
                }
            }
            // SALARY_DAILY의 경우 daysUntilPayday는 초기값인 null 유지

            // --- 6. 최종 DTO 조립 ---
            // 6-1. DTO에 맞게 nullable 공제 항목 계산
            Integer nationalPension = salaryInfo.getHasNationalPension() ? deductions.nationalPension() : null;
            Integer healthInsurance = salaryInfo.getHasHealthInsurance() ? deductions.healthInsurance() : null;
            Integer employmentInsurance = salaryInfo.getHasEmploymentInsurance() ? deductions.employmentInsurance() : null;
            Integer incomeTax = salaryInfo.getHasIncomeTax() ? deductions.incomeTax() : null;
            Integer netIncome = deductions.netIncome();

            WorkerMonthlyWorkplaceSummaryResponse summaryInfo = WorkerMonthlyWorkplaceSummaryResponse.builder()
                    .homeWorkplaceSummaryInfo(workerHomeWorkplaceSummaryInfo)
                    .daysUntilPayday(daysUntilPayday)
                    .totalWorkMinutes(totalWorkMinutes)
                    .dayTimeMinutes(totalWorkMinutes - totalNightMinutes)
                    .nightTimeMinutes(totalNightMinutes)
                    .restTimeMinutes(totalRestTimeMinutes)
                    .dayTimeIncome(dayTimeIncome)
                    .grossIncome(grossIncome)
                    .totalHolidayAllowance(salaryInfo.getHasHolidayAllowance() ? totalHolidayAllowance : null)
                    .totalNightAllowance(salaryInfo.getHasNightAllowance() ? totalNightAllowance : null)
                    .totalOvertimeAllowance(Boolean.TRUE.equals(salaryInfo.getHasOvertimeAllowance()) ? totalOvertimeAllowance : null)
                    .overtimeMinutes(totalOvertimeMinutes)
                    .totalPublicHolidayAllowance(Boolean.TRUE.equals(salaryInfo.getHasPublicHolidayAllowance()) ? totalPublicHolidayAllowance : null)
                    .publicHolidayMinutes(totalPublicHolidayMinutes)
                    .nationalPension(nationalPension)
                    .healthInsurance(healthInsurance)
                    .employmentInsurance(employmentInsurance)
                    .incomeTax(incomeTax)
                    .netIncome(netIncome)
                    .build();

            summaryResponseList.add(summaryInfo);
        }

        return summaryResponseList;
    }

    /// 사장님이 소유한 모든 사업장의 근무자 급여를 계산하고 저장합니다. (사장님 전용)
    @Transactional(readOnly = true)
    public List<OwnerMonthlyWorkplaceSummaryResponse> getOwnerMonthlyWorkplaceSummaryList(Long userId, int year, int month) {

        // 1. [쿼리 1] 해당 사용자가 소유한 모든 근무지를 조회합니다. (WorkplaceRepository 사용)
        List<Workplace> ownedWorkplaceList = workplaceRepository.findAllByOwnerId(userId);
        if (ownedWorkplaceList.isEmpty()) { return Collections.emptyList(); }

        List<Long> ownedWorkplaceIdList = ownedWorkplaceList.stream().map(Workplace::getId).toList();

        // 2. [쿼리 2] 모든 근무지에 속한 모든 Worker를 한 번에 조회합니다. (WorkerRepository 사용)
        List<Worker> allWorkerListInWorkplaces = workerRepository.findAllByWorkplaceIdListIn(ownedWorkplaceIdList);
        if (allWorkerListInWorkplaces.isEmpty()) { return Collections.emptyList(); }

        // 처리에 필요한 ID 리스트 추출
        List<Long> allWorkerIdList = allWorkerListInWorkplaces.stream().map(Worker::getId).toList();
        List<Long> allUserIdList = allWorkerListInWorkplaces.stream().map(Worker::getUserId).distinct().toList();

        // 3. [쿼리 3] DTO에 필요한 nickname을 위해 User를 조회합니다. (UserRepository 사용)
        Map<Long, User> userMap = userRepository.findAllByIdListIn(allUserIdList)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. [쿼리 4] 모든 근무자의 급여 정보를 한 번에 조회 (SalaryRepository 사용)
        Map<Long, Salary> salaryMap = salaryRepository.findAllByWorkerIdListIn(allWorkerIdList)
                .stream()
                .collect(Collectors.toMap(Salary::getWorkerId, salary -> salary));

        // 5. [쿼리 5] 해당 월의 모든 근무 기록을 한 번에 조회 (WorkRepository 사용)
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        Map<Long, List<Work>> workListByWorkerId = workRepository.findAllByWorkerIdListInAndDateRange(allWorkerIdList, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // 6. [In-Memory] 메모리에 로드된 데이터로 DTO 조립
        List<OwnerMonthlyWorkplaceSummaryResponse> summaryResponseList = new ArrayList<>();

        // 기준 루프를 '근무지'로 변경
        for (Workplace workplace : ownedWorkplaceList) {

            WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            List<OwnerMonthlyWorkerSummaryResponse> workerSummaryInfoList = new ArrayList<>();

            List<Worker> workersInThisWorkplace = allWorkerListInWorkplaces.stream()
                    .filter(w -> w.getWorkplaceId().equals(workplace.getId()))
                    .toList();

            for (Worker worker : workersInThisWorkplace) {
                Long workerId = worker.getId();

                List<Work> workerWorkList = workListByWorkerId.getOrDefault(workerId, Collections.emptyList());

                Salary salaryInfo = salaryMap.get(workerId);
                // Rule 3: 사장님처럼 급여 정보가 없는 근무자는 제외
                if (salaryInfo == null) {
                    continue;
                }

                // Rule 2: 근무 기록이 있는 모든 근무자에 대한 정보를 보여줘야 함
                // 따라서 근무 기록(workerWorkList)이 0건이면 제외
                if (workerWorkList.isEmpty()) {
                    continue;
                }

                // --- 급여 계산 ---

                // 1. 근무 시간/분 합산 (표시용)
                long totalNetWorkMinutes = workerWorkList.stream()
                        .mapToLong(work -> work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0)
                        .sum();

                long totalWorkHours = totalNetWorkMinutes / 60;

                // 2. 급여 계산 방식(SalaryCalculation)에 따른 세전 총 소득(grossMonthlyIncome) 계산
                int grossMonthlyIncome = 0;
                int fixedRate = (salaryInfo.getFixedRate() != null) ? salaryInfo.getFixedRate() : 0;

                if (salaryInfo.getSalaryCalculation() == SalaryCalculation.SALARY_CALCULATION_FIXED) {
                    // --- 고정급제 ---
                    // workerWorkList.isEmpty() 확인은 이미 위에서(continue) 처리되었으므로
                    // 불필요한 if-else 문을 제거하고 switch문만 남깁니다.
                    switch (salaryInfo.getSalaryType()) {
                        case SALARY_MONTHLY:
                            // 월급: 고정급(fixedRate)이 월급 총액
                            grossMonthlyIncome = fixedRate;
                            break;
                        case SALARY_WEEKLY:
                            // 주급: 고정급(fixedRate) * 해당 월의 주급 지급 횟수
                            DayOfWeek payDayOfWeek = salaryInfo.getSalaryDay();
                            int payDayCount = 0;
                            if (payDayOfWeek != null) {
                                LocalDate dateIterator = startDate; // 해당 월의 1일
                                while (!dateIterator.isAfter(endDate)) { // 해당 월의 마지막 날까지
                                    if (dateIterator.getDayOfWeek() == payDayOfWeek) {
                                        payDayCount++;
                                    }
                                    dateIterator = dateIterator.plusDays(1);
                                }
                            }
                            grossMonthlyIncome = fixedRate * payDayCount;
                            break;
                        case SALARY_DAILY:
                            // 일급: 고정급(fixedRate) * 해당 월의 근무일 수 (결근일 제외)
                            grossMonthlyIncome = fixedRate * countAttendedDays(workerWorkList);
                            break;
                    }
                } else {
                    // --- 시급제 (SALARY_CALCULATION_HOURLY) ---
                    // Work 레코드에 기록된 모든 세전 일급(grossIncome)을 합산
                    grossMonthlyIncome = workerWorkList.stream()
                            .mapToInt(work -> work.getGrossIncome() != null ? work.getGrossIncome() : 0)
                            .sum();
                }

                DeductionDetails deductions = calculateDeductions(grossMonthlyIncome, totalWorkHours, salaryInfo);

                User user = userMap.get(worker.getUserId());
                String nickname = (user != null) ? user.getNickname() : "탈퇴한 근무자";

                Integer netIncome = deductions.netIncome();

                // --- 근무자 요약 DTO (OwnerMonthlyWorkerSummaryResponse) 생성 ---
                OwnerMonthlyWorkerSummaryResponse workerSummary = OwnerMonthlyWorkerSummaryResponse.builder()
                        .nickname(nickname)
                        .totalWorkMinutes(totalNetWorkMinutes)
                        .grossIncome(grossMonthlyIncome)
                        .netIncome(netIncome)
                        .build();
                workerSummaryInfoList.add(workerSummary);
            }

            OwnerMonthlyWorkplaceSummaryResponse workplaceSummaryResponse = OwnerMonthlyWorkplaceSummaryResponse.builder()
                    .workplaceSummaryInfo(workplaceSummary)
                    .monthlyWorkerSummaryInfoList(workerSummaryInfoList)
                    .build();

            summaryResponseList.add(workplaceSummaryResponse);
        }

        return summaryResponseList;
    }

    /// `numerator / denominator`를 원 단위로 반올림한다(0.5 올림). 지급 항목용 — 값이 음수가 아니라는 전제다.
    private static int roundHalfUp(long numerator, long denominator) {
        return (int) ((numerator + denominator / 2) / denominator);
    }

    /// `amount × rate`를 10원 미만 절사한다. 공제 항목용.
    ///
    /// `double`로 곱하면 안 된다. 세전 50,000원 × 고용보험 0.009가 `449.99999999999994`가 되어
    /// 10원 단위로 자르면 450원이 아니라 **440원**이 된다. `BigDecimal.valueOf`는 요율의
    /// 십진 표기("0.009")를 그대로 쓰므로 정확하다.
    private static int truncateToTenWon(int amount, double rate) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(rate))
                .setScale(-1, RoundingMode.DOWN)
                .intValueExact();
    }

    /// 세전소득, 근무시간, 급여정보를 바탕으로 모든 공제액과 세후소득을 계산합니다.
    private DeductionDetails calculateDeductions(int grossIncome, long totalWorkHours, Salary salaryInfo) {
        int nationalPension = 0;
        int healthInsurance = 0;
        int employmentInsurance = 0;
        int incomeTax = 0;
        int localIncomeTax = 0;

        // 공제 항목은 각 공단·국세청 고지 방식대로 **10원 미만 절사**다 (스코프 3 Q7, 국고금 관리법 제47조).
        // 지급 항목(반올림)과 기준이 다르다 — 공제를 반올림하면 실제 고지액과 몇 원씩 어긋난다.
        if (isInsuranceApplicable(grossIncome, totalWorkHours)) {
            if (Boolean.TRUE.equals(salaryInfo.getHasNationalPension())) {
                nationalPension = truncateToTenWon(grossIncome, nationalPensionRate);
            }
            if (Boolean.TRUE.equals(salaryInfo.getHasHealthInsurance())) {
                int baseHealthInsurance = truncateToTenWon(grossIncome, healthInsuranceRate);
                // 장기요양은 소득이 아니라 **절사된 건강보험료**에 비율을 곱한다.
                int longTermCareInsurance = truncateToTenWon(baseHealthInsurance, longTermCareInsuranceRate);
                healthInsurance = baseHealthInsurance + longTermCareInsurance;
            }
            if (Boolean.TRUE.equals(salaryInfo.getHasEmploymentInsurance())) {
                employmentInsurance = truncateToTenWon(grossIncome, employmentInsuranceRate);
            }
        }

        if (Boolean.TRUE.equals(salaryInfo.getHasIncomeTax())) {
            incomeTax = truncateToTenWon(grossIncome, incomeTaxRate);
            localIncomeTax = truncateToTenWon(incomeTax, 0.1);
        }

        int totalDeductions = nationalPension + healthInsurance + employmentInsurance + incomeTax + localIncomeTax;
        int netIncome = grossIncome - totalDeductions;

        return new DeductionDetails(
                nationalPension,
                healthInsurance,
                employmentInsurance,
                incomeTax,
                localIncomeTax,
                totalDeductions,
                netIncome
        );
    }
}
