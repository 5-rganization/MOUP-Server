package com.moup.server.service;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.model.enums.SalaryCalculation;
import com.moup.server.model.enums.SalaryType;
import com.moup.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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

        boolean hasHolidayAllowance = (salary != null) && salary.getHasHolidayAllowance();
        boolean hasNightAllowance = (salary != null) && salary.getHasNightAllowance();

        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Work> weekWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startOfWeek, endOfWeek);

        // 주 총 근무시간을 계산하여 주휴수당 발생 조건(15시간 이상)을 확인합니다.
        long weeklyWorkMinutes = weekWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .mapToLong(work -> Duration.between(work.getStartTime(), work.getEndTime()).toMinutes() - (work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0))
                .sum();

        int weeklyHolidayAllowance = 0;
        if (weeklyWorkMinutes >= 15 * 60 && hasHolidayAllowance) {
            if (!weekWorks.isEmpty()) {
                // 주휴수당 발생 시, 주 평균 근무시간을 기준으로 수당을 계산합니다.
                double avgDailyWorkHours = (weeklyWorkMinutes / 60.0) / weekWorks.size();
                weeklyHolidayAllowance = (int) (avgDailyWorkHours * weekWorks.get(0).getHourlyRate());
            }
        }

        // 계산된 주휴수당을 근무일 수로 나누어 일급에 분배합니다.
        int dailyHolidayAllowance = weekWorks.isEmpty() ? 0 : weeklyHolidayAllowance / weekWorks.size();

        List<Work> updatedWorks = weekWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .map(work -> calculateDailyIncome(work, dailyHolidayAllowance, hasNightAllowance))
                .toList();

        // 해당 주의 모든 근무일에 대해 일급을 재계산합니다.
        if (!updatedWorks.isEmpty()) {
            workRepository.updateWorkWeekDetailsBatch(updatedWorks);
        }

        // 마지막으로, 월 전체의 '추정 세후 일급'을 다시 계산하여 캘린더 표시용 데이터를 업데이트합니다.
        recalculateEstimatedNetIncomeForMonth(workerId, date.getYear(), date.getMonthValue(), salary);
    }

    /// 하루 근무에 대한 세전 일급(각종 수당 포함)을 상세하게 계산합니다.
    public Work calculateDailyIncome(Work work, int dailyHolidayAllowance, boolean hasNightAllowance) {
        // end_time이 없으면 (아직 근무 중) 급여를 0으로 계산하고 반환
        if (work.getEndTime() == null) {
            return work.toBuilder()
                    .grossWorkMinutes(0)
                    .netWorkMinutes(0)
                    .nightWorkMinutes(0)
                    .basePay(0)
                    .nightAllowance(0)
                    .holidayAllowance(0)
                    .grossIncome(0)
                    .build();
        }

        LocalDateTime start = work.getStartTime();
        LocalDateTime end = work.getEndTime();
        int restMinutes = work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0;

        // --- 야간 및 연장 근무 시간 계산 ---
        long grossWorkMinutes = 0;
        long nightWorkMinutes = 0;

        // 근무 시간을 1분 단위로 순회하며 야간/연장 시간을 카운트합니다.
        LocalDateTime cursor = start;

        while (cursor.isBefore(end)) {
            // 1. 총 근무시간(Gross) 1분 추가
            grossWorkMinutes++;
            if (hasNightAllowance) {
                LocalTime cursorTime = cursor.toLocalTime();
                // 2. 22:00 이후 이거나, 06:00 이전일 때
                if (cursorTime.isAfter(NIGHT_START_TIME) || cursorTime.equals(NIGHT_START_TIME) || cursorTime.isBefore(NIGHT_END_TIME)) {
                    // 야간 근무시간 1분 추가
                    nightWorkMinutes++;
                }
            }
            cursor = cursor.plusMinutes(1);
        }

        long netWorkMinutes = grossWorkMinutes - restMinutes;
        if (netWorkMinutes < 0) netWorkMinutes = 0;

        // --- 수당 계산 ---
        int hourlyRate = (work.getHourlyRate() != null) ? work.getHourlyRate() : 0;
        int basePay = (int) (netWorkMinutes / 60.0 * hourlyRate);

        int nightAllowance = 0;
        if (hasNightAllowance) { nightAllowance = (int) (nightWorkMinutes / 60.0 * hourlyRate * 0.5); }

        int grossIncome = basePay + nightAllowance + dailyHolidayAllowance;

        // 계산된 모든 급여 항목을 Work 객체로 반환합니다.
        return work.toBuilder()
                .grossWorkMinutes((int) grossWorkMinutes)
                .netWorkMinutes((int) netWorkMinutes)
                .nightWorkMinutes((int) nightWorkMinutes)
                .basePay(basePay)
                .nightAllowance(nightAllowance)
                .holidayAllowance(dailyHolidayAllowance)
                .grossIncome(grossIncome)
                .hourlyRate(work.getHourlyRate())
                .build();
    }

    /// 캘린더에 표시될 '추정 세후 일급'을 월 단위로 재계산합니다.
    @Transactional
    public void recalculateEstimatedNetIncomeForMonth(Long workerId, int year, int month, Salary salaryInfo) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        List<Work> monthWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startDate, endDate);

        // 현재까지의 근무 기록을 바탕으로 예상 월급을 추정합니다.
        int currentGrossSum = monthWorks.stream()
                .mapToInt(work -> work.getGrossIncome() != null ? work.getGrossIncome() : 0)
                .sum();
        long daysWorked = monthWorks.size();

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
                        // 일급: 고정급 * 예상 총 근무일 수
                        estimatedMonthlyIncome = fixedRate * (int) daysWorked;
                        break;
                }
            }
        } else {
            // --- 시급제 ---
            estimatedMonthlyIncome = currentGrossSum;
        }

        long totalMinutesWorked = monthWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .mapToLong(work -> Duration.between(work.getStartTime(), work.getEndTime()).toMinutes() - (work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0))
                .sum();

        long estimatedTotalHours = 0;
        if (daysWorked > 0) { // 0으로 나누기 방지
            estimatedTotalHours = totalMinutesWorked / 60;
        }

        // 만약 예상 근무일이 0일이면 (예: 해당 월의 모든 근무가 삭제된 경우)
        // 기존에 계산된 estimatedNetIncome을 0으로 초기화해줍니다.
        if (daysWorked == 0) {
            workRepository.updateEstimatedNetIncomeToZeroByDateRange(workerId, startDate, endDate);
            return; // 0으로 나누기 오류를 방지하기 위해 여기서 종료
        }

        // 예상 월급 기준으로 월 총 공제액(4대보험, 소득세)을 추정합니다.
        int estimatedMonthlyDeduction = 0;

        if (salaryInfo != null) {
            DeductionDetails deductions = calculateDeductions(estimatedMonthlyIncome, estimatedTotalHours, salaryInfo);
            estimatedMonthlyDeduction = deductions.totalDeductions();
        }

        // 추정된 월 총 공제액을 예상 근무일로 나누어 '일일 추정 공제액'을 구합니다.
        int estimatedDailyDeduction = (int) (estimatedMonthlyDeduction / (double) daysWorked);

        // 해당 월의 모든 근무 기록에 '세전 일급 - 일일 추정 공제액'을 하여 '추정 세후 일급'을 업데이트합니다.
        workRepository.updateAllEstimatedNetIncomesForMonth(
                workerId,
                startDate,
                endDate,
                estimatedDailyDeduction
        );
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
                            // 일급: 고정급(fixedRate) * 해당 월의 근무일 수 (이미 workList.size() 기반)
                            grossIncome = fixedRate * workList.size();
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
            LocalDate today = LocalDate.now();
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
                            // 일급: 고정급(fixedRate) * 해당 월의 근무일 수
                            grossMonthlyIncome = fixedRate * workerWorkList.size();
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

    /// 세전소득, 근무시간, 급여정보를 바탕으로 모든 공제액과 세후소득을 계산합니다.
    private DeductionDetails calculateDeductions(int grossIncome, long totalWorkHours, Salary salaryInfo) {
        int nationalPension = 0;
        int healthInsurance = 0;
        int employmentInsurance = 0;
        int incomeTax = 0;
        int localIncomeTax = 0;

        if (isInsuranceApplicable(grossIncome, totalWorkHours)) {
            if (Boolean.TRUE.equals(salaryInfo.getHasNationalPension())) {
                nationalPension = (int) (grossIncome * nationalPensionRate);
            }
            if (Boolean.TRUE.equals(salaryInfo.getHasHealthInsurance())) {
                int baseHealthInsurance = (int) (grossIncome * healthInsuranceRate);
                int longTermCareInsurance = (int) (baseHealthInsurance * longTermCareInsuranceRate);
                healthInsurance = baseHealthInsurance + longTermCareInsurance;
            }
            if (Boolean.TRUE.equals(salaryInfo.getHasEmploymentInsurance())) {
                employmentInsurance = (int) (grossIncome * employmentInsuranceRate);
            }
        }

        if (Boolean.TRUE.equals(salaryInfo.getHasIncomeTax())) {
            incomeTax = (int) (grossIncome * incomeTaxRate);
            localIncomeTax = (int) (incomeTax * 0.1);
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