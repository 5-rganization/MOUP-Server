package com.moup.server.service;

import com.moup.server.common.Role;
import com.moup.server.exception.InvalidPermissionAccessException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final SalaryCalculationService salaryCalculationService;

    public BaseHomeResponse getHomeInfo(User user) {
        int nowYear = YearMonth.now().getYear();
        int nowMonth = YearMonth.now().getMonthValue();

        return switch (user.getRole()) {
            case ROLE_WORKER -> {
                List<WorkerMonthlyWorkplaceSummaryResponse> summaries =
                        salaryCalculationService.getWorkerMonthlyWorkplaceSummaries(user.getId(), nowYear, nowMonth);
                Integer totalSalary = summaries.stream()
                        .mapToInt(WorkerMonthlyWorkplaceSummaryResponse::getNetIncome)
                        .sum();

                yield WorkerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(totalSalary)
                        .workerMonthlyWorkplaceSummaryInfoList(summaries)
                        .build();
            }
            case ROLE_OWNER -> {
                List<OwnerMonthlyWorkplaceSummaryResponse> summaries =
                        salaryCalculationService.getOwnerMonthlyWorkplaceSummaries(user.getId(), nowYear, nowMonth);
                Integer totalSalary = summaries.stream()
                        .mapToInt(monthlySummary ->
                                monthlySummary.getMonthlyWorkerSummaryInfoList().stream()
                                        .mapToInt(OwnerMonthlyWorkerSummaryResponse::getNetIncome).sum()
                        ).sum();

                yield OwnerHomeResponse.builder()
                        .nowMonth(nowMonth)
                        .totalSalary(totalSalary)
                        .ownerMonthlyWorkplaceSummaryInfoList(summaries)
                        .build();
            }
            case ROLE_ADMIN ->
                // ADMIN의 홈 화면이 따로 없다면 명시적으로 예외 처리
                    throw new InvalidPermissionAccessException();
        };
    }
}
