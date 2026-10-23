package com.moup.domain.user.application;

import com.moup.domain.user.dto.BaseHomeResponse;
import com.moup.domain.user.dto.OwnerHomeResponse;
import com.moup.domain.user.dto.OwnerMonthlyWorkerSummaryResponse;
import com.moup.domain.user.dto.OwnerMonthlyWorkplaceSummaryResponse;
import com.moup.domain.user.dto.WorkerHomeResponse;
import com.moup.domain.user.dto.WorkerMonthlyWorkplaceSummaryResponse;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.domain.user.domain.User;
import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.salary.application.SalaryCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final SalaryCalculationService salaryCalculationService;
    private final RoutineService routineService;

    @Transactional(readOnly = true)
    public BaseHomeResponse getHomeInfo(User user, LocalDate date) {
        // --- 현재 달 정보 ---
        YearMonth currentYearMonth = YearMonth.from(date);
        int nowYear = currentYearMonth.getYear();
        int nowMonth = currentYearMonth.getMonthValue();
        int todayRoutineCount = routineService.getTodayTotalRoutineCount(user.getId(), date);

        // --- 이전 달 정보 ---
        YearMonth previousYearMonth = currentYearMonth.minusMonths(1);
        int prevYear = previousYearMonth.getYear();
        int prevMonth = previousYearMonth.getMonthValue();

        // --- 역할별 처리 ---
        return switch (user.getRole()) {
            case ROLE_WORKER -> {
                // 현재 달 급여 계산
                List<WorkerMonthlyWorkplaceSummaryResponse> currentSummaries =
                        salaryCalculationService.getWorkerMonthlyWorkplaceSummaryList(user.getId(), nowYear, nowMonth);
                int currentTotalSalary = currentSummaries.stream()
                        .mapToInt(WorkerMonthlyWorkplaceSummaryResponse::getNetIncome)
                        .sum();

                // 이전 달 급여 계산 (없으면 0)
                List<WorkerMonthlyWorkplaceSummaryResponse> previousSummaries =
                        salaryCalculationService.getWorkerMonthlyWorkplaceSummaryList(user.getId(), prevYear, prevMonth);
                int previousTotalSalary = previousSummaries.stream()
                        .mapToInt(WorkerMonthlyWorkplaceSummaryResponse::getNetIncome)
                        .sum(); // 비어있으면 sum()은 0 반환

                // 차액 계산
                int salaryDifference = currentTotalSalary - previousTotalSalary;

                yield WorkerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(currentTotalSalary)
                        .prevMonthSalaryDiff(salaryDifference)
                        .todayRoutineCounts(todayRoutineCount)
                        .workerMonthlyWorkplaceSummaryInfoList(currentSummaries)
                        .build();
            }
            case ROLE_OWNER -> {
                // 현재 달 인건비 계산
                List<OwnerMonthlyWorkplaceSummaryResponse> currentSummaries =
                        salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(user.getId(), nowYear, nowMonth);
                int currentTotalSalary = sumLaborCost(currentSummaries);

                // 이전 달 인건비 계산 (없으면 0)
                List<OwnerMonthlyWorkplaceSummaryResponse> previousSummaries =
                        salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(user.getId(), prevYear, prevMonth);
                int previousTotalSalary = sumLaborCost(previousSummaries); // 비어있으면 0

                // 차액 계산
                int salaryDifference = currentTotalSalary - previousTotalSalary;

                yield OwnerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(currentTotalSalary)
                        .prevMonthSalaryDiff(salaryDifference)
                        .todayRoutineCounts(todayRoutineCount)
                        .ownerMonthlyWorkplaceSummaryInfoList(currentSummaries)
                        .build();
            }
            case ROLE_ADMIN ->
                    throw new InvalidPermissionAccessException();
        };
    }

    /// 사장님 인건비 = 근무자 **세전** 소득 합계.
    ///
    /// 세후는 알바생 통장에 들어가는 금액일 뿐이고, 원천징수한 세금·근로자 몫 4대보험도
    /// 사장님이 대신 납부하므로 사장님이 내는 돈이다. 예전에는 세후를 더해 실제 부담보다
    /// 약 17% 작게 보였다. 사업주 몫 4대보험은 넣지 않는다 — 산재(업종별)·고용안정(규모별)
    /// 요율을 몰라 추정치가 된다.
    ///
    /// 이번 달과 지난달이 반드시 같은 기준이어야 한다. 한쪽만 바뀌면 전월 대비 차이에
    /// 세전·세후 차액이 그대로 섞인다.
    private static int sumLaborCost(List<OwnerMonthlyWorkplaceSummaryResponse> summaries) {
        return summaries.stream()
                .flatMap(summary -> summary.getMonthlyWorkerSummaryInfoList().stream())
                .mapToInt(OwnerMonthlyWorkerSummaryResponse::getGrossIncome)
                .sum();
    }
}
