package com.moup.server.service;

import com.moup.server.exception.InvalidPermissionAccessException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final SalaryCalculationService salaryCalculationService;
    private final RoutineService routineService;

    public BaseHomeResponse getHomeInfo(User user, LocalDate date) {
        int nowYear = date.getYear();
        int nowMonth = date.getMonthValue();

        int todayRoutineCount = routineService.getTodayTotalRoutineCount(user.getId(), date);

        return switch (user.getRole()) {
            case ROLE_WORKER -> {
                List<WorkerMonthlyWorkplaceSummaryResponse> summaries =
                        salaryCalculationService.getWorkerMonthlyWorkplaceSummaryList(user.getId(), nowYear, nowMonth);
                Integer totalSalary = summaries.stream()
                        .mapToInt(WorkerMonthlyWorkplaceSummaryResponse::getNetIncome)
                        .sum();

                yield WorkerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(totalSalary)
                        .todayRoutineCounts(todayRoutineCount)
                        .workerMonthlyWorkplaceSummaryInfoList(summaries)
                        .build();
            }
            case ROLE_OWNER -> {
                List<OwnerMonthlyWorkplaceSummaryResponse> summaries =
                        salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(user.getId(), nowYear, nowMonth);
                Integer totalSalary = summaries.stream()
                        .mapToInt(monthlySummary ->
                                monthlySummary.getMonthlyWorkerSummaryInfoList().stream()
                                        .mapToInt(OwnerMonthlyWorkerSummaryResponse::getNetIncome).sum()
                        ).sum();

                yield OwnerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(totalSalary)
                        .todayRoutineCounts(todayRoutineCount)
                        .ownerMonthlyWorkplaceSummaryInfoList(summaries)
                        .build();
            }
            case ROLE_ADMIN ->
                    throw new InvalidPermissionAccessException();
        };
    }
}
