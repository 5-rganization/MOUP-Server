package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.user.application.HomeService;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.dto.BaseHomeResponse;
import com.moup.domain.user.dto.OwnerMonthlyWorkerSummaryResponse;
import com.moup.domain.user.dto.OwnerMonthlyWorkplaceSummaryResponse;
import com.moup.global.common.type.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/// 사장님 홈 "인건비"는 근무자 **세전** 소득 합계다 (스코프 3 Q8 · 스코프 4 I10).
@ExtendWith(MockitoExtension.class)
class HomeLaborCostTest {

    private static final long OWNER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @Mock
    private SalaryCalculationService salaryCalculationService;

    @Mock
    private RoutineService routineService;

    @InjectMocks
    private HomeService homeService;

    private static OwnerMonthlyWorkplaceSummaryResponse workplace(OwnerMonthlyWorkerSummaryResponse... workers) {
        return OwnerMonthlyWorkplaceSummaryResponse.builder()
                .monthlyWorkerSummaryInfoList(List.of(workers))
                .build();
    }

    private static OwnerMonthlyWorkerSummaryResponse worker(int gross, int net) {
        return OwnerMonthlyWorkerSummaryResponse.builder()
                .nickname("알바생")
                .totalWorkMinutes(0L)
                .grossIncome(gross)
                .netIncome(net)
                .build();
    }

    @Test
    @DisplayName("인건비는 세후가 아니라 세전 합계이고, 전월 대비 차이도 같은 기준으로 계산한다")
    void laborCostIsGrossSumForBothMonths() {
        User owner = User.builder().id(OWNER_ID).role(Role.ROLE_OWNER).build();
        when(routineService.getTodayTotalRoutineCount(anyLong(), eq(TODAY))).thenReturn(0);
        // 이번 달: 근무지 2곳에 걸쳐 세전 2,000,000 + 500,000 (세후는 각각 더 작다)
        when(salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(OWNER_ID, 2026, 10))
                .thenReturn(List.of(
                        workplace(worker(2_000_000, 1_800_000)),
                        workplace(worker(500_000, 480_000))));
        // 지난달: 세전 1,000,000
        when(salaryCalculationService.getOwnerMonthlyWorkplaceSummaryList(OWNER_ID, 2026, 9))
                .thenReturn(List.of(workplace(worker(1_000_000, 900_000))));

        BaseHomeResponse response = homeService.getHomeInfo(owner, TODAY);

        assertEquals(2_500_000, response.getTotalSalary());
        // 지난달만 세후로 남아 있으면 2,500,000 - 900,000 = 1,600,000이 된다
        assertEquals(1_500_000, response.getPrevMonthSalaryDiff());
    }
}
