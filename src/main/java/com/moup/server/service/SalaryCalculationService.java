package com.moup.server.service;

import com.moup.server.exception.SalaryWorkerNotFoundException;
import com.moup.server.model.entity.MonthlySalary;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Work;
import com.moup.server.repository.MonthlySalaryRepository;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryCalculationService {

    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final MonthlySalaryRepository monthlySalaryRepository;

    // --- application.properties에서 주입받는 설정값들 ---
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
    /// 근무가 생성/업데이트 될 때마다 호출되어 주 전체에 영향을 미치는 값을 업데이트합니다.
    @Transactional
    public void recalculateWorkWeek(Long workerId, LocalDate date) {
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Work> weekWorks = workRepository.findAllByWorkerIdAndDateRange(workerId, startOfWeek, endOfWeek);
        if (weekWorks.isEmpty()) return;

        // 주 총 근무시간을 계산하여 주휴수당 발생 조건(15시간 이상)을 확인합니다.
        long weeklyWorkMinutes = weekWorks.stream()
                .mapToLong(w -> Duration.between(w.getStartTime(), w.getEndTime()).toMinutes() - (w.getRestTimeMinutes() != null ? w.getRestTimeMinutes() : 0))
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
                .map(work -> calculateDailyIncome(work, dailyHolidayAllowance))
                .toList();

        // 해당 주의 모든 근무일에 대해 일급을 재계산합니다.
        updatedWorks.forEach(workRepository::update);

        // 마지막으로, 월 전체의 '추정 세후 일급'을 다시 계산하여 캘린더 표시용 데이터를 업데이트합니다.
        recalculateEstimatedNetIncomeForMonth(workerId, date.getYear(), date.getMonthValue());
    }

    /// 하루 근무에 대한 세전 일급(각종 수당 포함)을 상세하게 계산합니다.
    private Work calculateDailyIncome(Work work, int dailyHolidayAllowance) {
        LocalDateTime start = work.getStartTime();
        LocalDateTime end = work.getEndTime();
        int restMinutes = work.getRestTimeMinutes() != null ? work.getRestTimeMinutes() : 0;

        // --- 야간 및 연장 근무 시간 계산 ---
        long nightWorkMinutes = 0;
        long overtimeMinutes = 0;
        final long dailyWorkHourLimit = 8 * 60; // 8시간(분 단위)
        long regularWorkMinutes = 0; // 휴게시간 제외 순수 근무시간

        // 근무 시간을 1분 단위로 순회하며 야간/연장 시간을 카운트합니다.
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            regularWorkMinutes++;
            LocalTime cursorTime = cursor.toLocalTime();
            if (cursorTime.isAfter(NIGHT_START_TIME) || cursorTime.equals(NIGHT_START_TIME) || cursorTime.isBefore(NIGHT_END_TIME)) {
                nightWorkMinutes++;
            }
            cursor = cursor.plusMinutes(1);
        }

        regularWorkMinutes -= restMinutes;

        // 순수 근무시간이 8시간을 넘으면 연장 근무로 처리합니다.
        if (regularWorkMinutes > dailyWorkHourLimit) {
            overtimeMinutes = regularWorkMinutes - dailyWorkHourLimit;
        }

        // --- 수당 계산 ---
        // 기본급: (순수 근무시간 - 연장근무 시간)에 대한 급여
        int basePay = (int) ((regularWorkMinutes - overtimeMinutes) / 60.0 * work.getHourlyRate());
        // 야간수당 및 연장수당: 각각의 시간에 대해 50% 가산 (시급 * 0.5)
        int nightAllowance = (int) (nightWorkMinutes / 60.0 * work.getHourlyRate() * 0.5);
        int overtimeAllowance = (int) (overtimeMinutes / 60.0 * work.getHourlyRate() * 0.5);

        // 계산된 모든 급여 항목을 Work 객체로 반환합니다.
        return work.toBuilder()
                .basePay(basePay)
                .nightAllowance(nightAllowance)
                .overtimeAllowance(overtimeAllowance)
                .holidayAllowance(dailyHolidayAllowance)
                .grossIncome(basePay + nightAllowance + overtimeAllowance + dailyHolidayAllowance)
                .build();
    }

    /// 캘린더에 표시될 '추정 세후 일급'을 월 단위로 재계산합니다.
    /// 현재까지의 근무 기록을 바탕으로 예상 월급과 예상 공제액을 계산하여 반영합니다.
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

        long totalMinutesWorked = monthWorks.stream().mapToLong(w -> Duration.between(w.getStartTime(), w.getEndTime()).toMinutes() - (w.getRestTimeMinutes() != null ? w.getRestTimeMinutes() : 0)).sum();
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

    /// 월급 명세서 등에서 호출하는 '최종 월급 정산' 메서드입니다.
    /// 한 달의 모든 근무 기록을 합산하여 정확한 공제액과 실지급액을 계산하고 DB에 저장합니다.
    @Transactional
    public MonthlySalary calculateAndSaveMonthlySalary(Long workerId, int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        List<Work> works = workRepository.findAllByWorkerIdAndDateRange(workerId, startDate, endDate);
        if (works.isEmpty()) return null;

        Salary salaryInfo = salaryRepository.findByWorkerId(workerId)
                .orElseThrow(SalaryWorkerNotFoundException::new);

        // 월 총 세전 소득과 월 총 근무 시간을 정확하게 계산합니다.
        int grossMonthlyIncome = works.stream().mapToInt(Work::getGrossIncome).sum();
        long totalWorkMinutes = works.stream().mapToLong(w -> Duration.between(w.getStartTime(), w.getEndTime()).toMinutes() - (w.getRestTimeMinutes() != null ? w.getRestTimeMinutes() : 0)).sum();
        long totalWorkHours = totalWorkMinutes / 60;

        int nationalPension = 0;
        int healthInsurance = 0;
        int employmentInsurance = 0;
        int incomeTax = 0;
        int localIncomeTax = 0;

        // 월 총 근무시간 기준으로 보험 가입 대상 여부를 판단하고, 정확한 보험료를 계산합니다.
        if (totalWorkHours >= insuranceMinHours) {
            if (salaryInfo.getHasNationalPension()) {
                nationalPension = (int) (grossMonthlyIncome * nationalPensionRate);
            }
            if (salaryInfo.getHasHealthInsurance()) {
                int baseHealthInsurance = (int) (grossMonthlyIncome * healthInsuranceRate);
                int longTermCareInsurance = (int) (baseHealthInsurance * longTermCareInsuranceRate);
                healthInsurance = baseHealthInsurance + longTermCareInsurance;
            }
            if (salaryInfo.getHasEmploymentInsurance()) {
                employmentInsurance = (int) (grossMonthlyIncome * employmentInsuranceRate);
            }
        }

        // 정확한 소득세를 계산합니다.
        if (salaryInfo.getHasIncomeTax()) {
            incomeTax = (int) (grossMonthlyIncome * incomeTaxRate);
            localIncomeTax = (int) (incomeTax * 0.1);
        }

        int totalDeductions = nationalPension + healthInsurance + employmentInsurance + incomeTax + localIncomeTax;
        int netIncome = grossMonthlyIncome - totalDeductions;

        // 최종 정산 내역을 MonthlySalary 객체로 만들어 저장합니다.
        MonthlySalary monthlySalary = MonthlySalary.builder()
                .workerId(workerId)
                .salaryMonth(targetMonth) // MonthlySalary 엔티티 필드 타입에 맞게 수정 필요
                .grossIncome(grossMonthlyIncome)
                .nationalPension(nationalPension)
                .healthInsurance(healthInsurance)
                .employmentInsurance(employmentInsurance)
                .incomeTax(incomeTax)
                .localIncomeTax(localIncomeTax)
                .netIncome(netIncome)
                .build();

        // TODO: 이미 해당 월의 정산 내역이 있다면 UPDATE, 없다면 INSERT 하는 로직(UPSERT) 구현 필요
        monthlySalaryRepository.create(monthlySalary);

        return monthlySalary;
    }
}