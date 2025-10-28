package com.moup.server.service;

import com.moup.server.exception.InvalidPermissionAccessException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
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
                int currentTotalSalary = currentSummaries.stream()
                        .mapToInt(monthlySummary ->
                                monthlySummary.getMonthlyWorkerSummaryInfoList().stream()
                                        .mapToInt(OwnerMonthlyWorkerSummaryResponse::getNetIncome).sum()
                        ).sum();

                // 이전 달 인건비 계산 (없으면 0)
                List<OwnerMonthlyWorkplaceSummaryResponse> previousSummaries =
                        salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(user.getId(), prevYear, prevMonth);
                int previousTotalSalary = previousSummaries.stream()
                        .mapToInt(monthlySummary ->
                                monthlySummary.getMonthlyWorkerSummaryInfoList().stream()
                                        .mapToInt(OwnerMonthlyWorkerSummaryResponse::getNetIncome).sum()
                        ).sum(); // 비어있으면 sum()은 0 반환

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
}
