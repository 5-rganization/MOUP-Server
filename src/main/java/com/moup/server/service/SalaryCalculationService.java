package com.moup.server.service;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
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
    public void recalculateWorkWeek(Long workerId, LocalDate date) {
        boolean hasNightAllowance = salaryRepository.findByWorkerId(workerId)
                .map(Salary::getHasNightAllowance)
                .orElse(false);

        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Work> weekWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startOfWeek, endOfWeek);
        if (weekWorks.isEmpty()) return;

        // 주 총 근무시간을 계산하여 주휴수당 발생 조건(15시간 이상)을 확인합니다.
        long weeklyWorkMinutes = weekWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .mapToLong(work -> Duration.between(work.getStartTime(), work.getEndTime()).toMinutes() - (work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0))
                .sum();

        int weeklyHolidayAllowance = 0;
        if (weeklyWorkMinutes >= 15 * 60) {
            // 주휴수당 발생 시, 주 평균 근무시간을 기준으로 수당을 계산합니다.
            double avgDailyWorkHours = (weeklyWorkMinutes / 60.0) / weekWorks.size();
            weeklyHolidayAllowance = (int) (avgDailyWorkHours * weekWorks.get(0).getHourlyRate());
        }

        // 계산된 주휴수당을 근무일 수로 나누어 일급에 분배합니다.
        int dailyHolidayAllowance = weekWorks.isEmpty() ? 0 : weeklyHolidayAllowance / weekWorks.size();

        List<Work> updatedWorks = weekWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .map(work -> calculateDailyIncome(work, dailyHolidayAllowance, hasNightAllowance))
                .toList();

        // 해당 주의 모든 근무일에 대해 일급을 재계산합니다.
        updatedWorks.forEach(workRepository::update);

        // 마지막으로, 월 전체의 '추정 세후 일급'을 다시 계산하여 캘린더 표시용 데이터를 업데이트합니다.
        recalculateEstimatedNetIncomeForMonth(workerId, date.getYear(), date.getMonthValue());
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
            grossWorkMinutes++; // 1. 총 근무시간(Gross) 1분 추가

            if (hasNightAllowance) {
                LocalTime cursorTime = cursor.toLocalTime();
                // 2. 22:00 이후 이거나, 06:00 이전일 때
                if (cursorTime.isAfter(NIGHT_START_TIME) || cursorTime.equals(NIGHT_START_TIME) || cursorTime.isBefore(NIGHT_END_TIME)) {
                    nightWorkMinutes++; // 야간 근무시간 1분 추가
                }
            }
            cursor = cursor.plusMinutes(1);
        }

        long netWorkMinutes = grossWorkMinutes - restMinutes;
        if (netWorkMinutes < 0) netWorkMinutes = 0;

        // --- 수당 계산 ---
        int basePay = (int) (netWorkMinutes / 60.0 * work.getHourlyRate());

        int nightAllowance = 0;
        if (hasNightAllowance) {
            nightAllowance = (int) (nightWorkMinutes / 60.0 * work.getHourlyRate() * 0.5);
        }

        // 계산된 모든 급여 항목을 Work 객체로 반환합니다.
        return work.toBuilder()
                .grossWorkMinutes((int) grossWorkMinutes)
                .netWorkMinutes((int) netWorkMinutes)
                .nightWorkMinutes((int) nightWorkMinutes)
                .basePay(basePay)
                .nightAllowance(nightAllowance)
                .holidayAllowance(dailyHolidayAllowance)
                .grossIncome(basePay + nightAllowance + dailyHolidayAllowance)
                .build();
    }

    /// 캘린더에 표시될 '추정 세후 일급'을 월 단위로 재계산합니다.
    @Transactional
    public void recalculateEstimatedNetIncomeForMonth(Long workerId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        List<Work> monthWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startDate, endDate);
        if (monthWorks.isEmpty()) return;

        // 현재까지의 근무 기록을 바탕으로 예상 월급을 추정합니다.
        int currentGrossSum = monthWorks.stream().mapToInt(Work::getGrossIncome).sum();
        long daysWorked = monthWorks.size();

        long daysInMonth = endDate.getDayOfMonth();
        long passedDays = LocalDate.now().getDayOfMonth();
        if (LocalDate.now().getMonthValue() != month || LocalDate.now().getYear() != year) {
            passedDays = daysInMonth; // 현재 달이 아니면, 해당 월의 전체 일수를 기준으로 삼음
        }

        double workDayRatio = (double) daysWorked / passedDays;
        long estimatedTotalWorkingDays = Math.round(workDayRatio * daysInMonth);
        if (estimatedTotalWorkingDays == 0) estimatedTotalWorkingDays = 1;

        int estimatedMonthlyIncome = (int) ((double) currentGrossSum / daysWorked * estimatedTotalWorkingDays);

        long totalMinutesWorked = monthWorks.stream()
                .filter(work -> work.getEndTime() != null)
                .mapToLong(work -> Duration.between(work.getStartTime(), work.getEndTime()).toMinutes() - (work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0))
                .sum();
        long estimatedTotalHours = (long)((double) totalMinutesWorked / daysWorked * estimatedTotalWorkingDays / 60.0);

        // 예상 월급 기준으로 월 총 공제액(4대보험, 소득세)을 추정합니다.
        int estimatedMonthlyDeduction = 0;
        if (isInsuranceApplicable(estimatedMonthlyIncome, estimatedTotalHours)) {
            estimatedMonthlyDeduction += (int) (estimatedMonthlyIncome * nationalPensionRate);
            estimatedMonthlyDeduction += (int) (estimatedMonthlyIncome * (healthInsuranceRate * (1 + longTermCareInsuranceRate)));
            estimatedMonthlyDeduction += (int) (estimatedMonthlyIncome * employmentInsuranceRate);
        }
        estimatedMonthlyDeduction += (int) ((estimatedMonthlyIncome * incomeTaxRate) * 1.1); // 지방소득세 10% 포함

        // 추정된 월 총 공제액을 예상 근무일로 나누어 '일일 추정 공제액'을 구합니다.
        int estimatedDailyDeduction = (int) (estimatedMonthlyDeduction / (double) estimatedTotalWorkingDays);

        // 해당 월의 모든 근무 기록에 '세전 일급 - 일일 추정 공제액'을 하여 '추정 세후 일급'을 업데이트합니다.
        monthWorks.forEach(work -> {
            int netIncome = work.getGrossIncome() - estimatedDailyDeduction;
            Work updatedWork = work.toBuilder()
                    .estimatedNetIncome(Math.max(netIncome, 0))
                    .build();

            workRepository.update(updatedWork);
        });
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

        List<WorkerMonthlyWorkplaceSummaryResponse> summaryList = new ArrayList<>();
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        // 2. N+1 방지를 위해 필요한 정보를 미리 조회합니다.
        List<Long> workerIdList = userWorkerList.stream().map(Worker::getId).toList();

        // [쿼리 1] 모든 Salary 정보
        Map<Long, Salary> salaryMap = salaryRepository.findAllByWorkerIdIn(workerIdList)
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

            // [필터 2] 해당 월에 근무 기록이 없으면 건너뜁니다.
            List<Work> workList = workMap.getOrDefault(workerId, Collections.emptyList());
            if (workList.isEmpty()) { continue; }

            // [필터 3] 근무지 정보 조회
            Workplace workplace = workplaceMap.get(worker.getWorkplaceId());
            WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            // --- SalarySummaryResponse DTO 생성 ---
            SalarySummaryResponse salarySummaryInfo = SalarySummaryResponse.builder()
                    .salaryType(salaryInfo.getSalaryType())
                    .salaryCalculation(salaryInfo.getSalaryCalculation())
                    .hourlyRate(salaryInfo.getHourlyRate())
                    .fixedRate(salaryInfo.getFixedRate())
                    .salaryDate(salaryInfo.getSalaryDate())
                    .salaryDay(salaryInfo.getSalaryDay())
                    .build();


            // 4. 시간 및 수당 계산 (DB에 저장된 값을 합산)
            long totalWorkMinutes = workList.stream()
                    .mapToLong(work -> work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0)
                    .sum();

            long totalNightMinutes = workList.stream()
                    .mapToLong(work -> work.getNightWorkMinutes() != null ? work.getNightWorkMinutes() : 0)
                    .sum();

            long totalRestTimeMinutes = workList.stream()
                    .mapToLong(work -> work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0)
                    .sum();

            int totalHolidayAllowance = workList.stream()
                    .mapToInt(work -> work.getHolidayAllowance() != null ? work.getHolidayAllowance() : 0)
                    .sum();

            int grossIncome = workList.stream()
                    .mapToInt(work -> work.getGrossIncome() != null ? work.getGrossIncome() : 0)
                    .sum();

            long totalWorkHours = totalWorkMinutes / 60;

            // --- 5. 공제액 계산 ---
            DeductionDetails deductions = calculateDeductions(grossIncome, totalWorkHours, salaryInfo);

            // --- 6. 최종 DTO 조립 ---
            WorkerMonthlyWorkplaceSummaryResponse summary = WorkerMonthlyWorkplaceSummaryResponse.builder()
                    .workplaceSummaryInfo(workplaceSummaryInfo)
                    .salarySummaryInfo(salarySummaryInfo)
                    .totalWorkMinutes(totalWorkMinutes)
                    .dayTimeMinutes(totalWorkMinutes - totalNightMinutes) // netWorkMinutes - nightWorkMinutes = dayTimeMinutes
                    .nightTimeMinutes(totalNightMinutes)
                    .restTimeMinutes(totalRestTimeMinutes)
                    .totalHolidayAllowance(totalHolidayAllowance)
                    .grossIncome(grossIncome)
                    .fourMajorInsurances(deductions.nationalPension() + deductions.healthInsurance() + deductions.employmentInsurance())
                    .incomeTax(deductions.incomeTax())
                    .netIncome(deductions.netIncome())
                    .build();

            summaryList.add(summary);
        }

        return summaryList;
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
        Map<Long, Salary> salaryMap = salaryRepository.findAllByWorkerIdIn(allWorkerIdList)
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
        List<OwnerMonthlyWorkplaceSummaryResponse> responseList = new ArrayList<>();

        // 기준 루프를 '근무지'로 변경
        for (Workplace workplace : ownedWorkplaceList) {

            WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            List<OwnerMonthlyWorkerSummaryResponse> workerSummaryList = new ArrayList<>();

            List<Worker> workersInThisWorkplace = allWorkerListInWorkplaces.stream()
                    .filter(w -> w.getWorkplaceId().equals(workplace.getId()))
                    .toList();

            for (Worker worker : workersInThisWorkplace) {
                Long workerId = worker.getId();

                List<Work> workerWorkList = workListByWorkerId.getOrDefault(workerId, Collections.emptyList());
                if (workerWorkList.isEmpty()) { continue; }

                Salary salaryInfo = salaryMap.get(workerId);
                if (salaryInfo == null) { continue; }

                // --- 급여 계산 (수정됨) ---
                // [수정 없음] 세전 총소득은 미리 계산된 값을 합산
                int grossMonthlyIncome = workerWorkList.stream()
                        .mapToInt(Work::getGrossIncome)
                        .sum();

                long totalNetWorkMinutes = workerWorkList.stream()
                        .mapToLong(Work::getNetWorkMinutes)
                        .sum();

                long totalWorkHours = totalNetWorkMinutes / 60;

                DeductionDetails deductions = calculateDeductions(grossMonthlyIncome, totalWorkHours, salaryInfo);

                User user = userMap.get(worker.getUserId());
                String nickname = (user != null) ? user.getNickname() : "탈퇴한 근무자";

                // --- 근무자 요약 DTO (OwnerMonthlyWorkerSummaryResponse) 생성 ---
                OwnerMonthlyWorkerSummaryResponse workerSummary = OwnerMonthlyWorkerSummaryResponse.builder()
                        .nickname(nickname)
                        .totalWorkMinutes(totalNetWorkMinutes)
                        .netIncome(deductions.netIncome())
                        .build();
                workerSummaryList.add(workerSummary);
            }

            OwnerMonthlyWorkplaceSummaryResponse workplaceSummaryResponse = OwnerMonthlyWorkplaceSummaryResponse.builder()
                    .workplaceSummaryInfo(workplaceSummary)
                    .monthlyWorkerSummaryInfoList(workerSummaryList)
                    .build();

            responseList.add(workplaceSummaryResponse);
        }

        return responseList;
    }

    /// 세전소득, 근무시간, 급여정보를 바탕으로 모든 공제액과 세후소득을 계산합니다.
    private DeductionDetails calculateDeductions(int grossIncome, long totalWorkHours, Salary salaryInfo) {
        int nationalPension = 0;
        int healthInsurance = 0;
        int employmentInsurance = 0;
        int incomeTax = 0;
        int localIncomeTax = 0;

        if (totalWorkHours >= insuranceMinHours) {
            if (salaryInfo.getHasNationalPension()) {
                nationalPension = (int) (grossIncome * nationalPensionRate);
            }
            if (salaryInfo.getHasHealthInsurance()) {
                int baseHealthInsurance = (int) (grossIncome * healthInsuranceRate);
                int longTermCareInsurance = (int) (baseHealthInsurance * longTermCareInsuranceRate);
                healthInsurance = baseHealthInsurance + longTermCareInsurance;
            }
            if (salaryInfo.getHasEmploymentInsurance()) {
                employmentInsurance = (int) (grossIncome * employmentInsuranceRate);
            }
        }

        if (salaryInfo.getHasIncomeTax()) {
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