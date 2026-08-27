package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Phase 5 / 3-6 회귀 테스트 — 반복 근무 삭제 후 **걸친 모든 주**가 재계산되는가.
///
/// `recalculateWorkWeek`은 인자로 받은 날짜가 속한 한 주만 다시 계산한다.
/// 반복 삭제는 최대 365일치를 지우는데 재계산은 1주만 하고 있어, 나머지 주의
/// `holiday_allowance`/`gross_income`이 삭제 전 값 그대로 남았다.
/// 주휴수당은 주 15시간 임계에 걸리므로 근무가 사라지면 그 주 금액이 달라진다.
@ExtendWith(MockitoExtension.class)
class RecurringWorkRecalculationTest {

    private static final Long REQUESTER_USER_ID = 1L;
    private static final Long WORKER_ID = 10L;
    private static final Long WORK_ID = 100L;
    private static final String GROUP_ID = "group-1";

    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoutineService routineService;
    @Mock private SalaryCalculationService salaryCalculationService;
    @Mock private com.moup.domain.routine.mapper.RoutineTaskCompletionRepository routineTaskCompletionRepository;

    // 실제 구현을 쓴다. 권한 검증까지 함께 통과해야 의미가 있다.
    // userRepository는 @Mock이라 isWithdrawn이 기본값 false를 돌려준다 = 소유자 정상.
    private PermissionVerifyUtil permissionVerifyUtil;

    private WorkService workService;

    private WorkService service() {
        if (workService == null) {
            permissionVerifyUtil = new PermissionVerifyUtil(userRepository);
            workService = new WorkService(workRepository, salaryRepository, workerRepository,
                    workplaceRepository, userRepository, routineService,
                    salaryCalculationService, permissionVerifyUtil, routineTaskCompletionRepository);
        }
        return workService;
    }

    @Test
    @DisplayName("반복 삭제 시 삭제 범위에 걸친 모든 주가 재계산된다")
    void 삭제_범위의_모든_주가_재계산된다() {
        LocalDate deleteFrom = LocalDate.of(2025, 11, 3);   // 월요일
        LocalDate groupLast = LocalDate.of(2025, 11, 28);   // 4주 뒤 금요일

        givenWork(deleteFrom);
        when(workRepository.findLastWorkDateByRepeatGroupId(GROUP_ID)).thenReturn(Optional.of(groupLast));
        when(workRepository.deleteRecurringWorkFromDate(GROUP_ID, deleteFrom)).thenReturn(12);
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.empty());

        service().deleteRecurringWorkIncludingDate(REQUESTER_USER_ID, WORK_ID);

        ArgumentCaptor<LocalDate> weeks = ArgumentCaptor.forClass(LocalDate.class);
        verify(salaryCalculationService, atLeastOnce())
                .recalculateWorkWeek(anyLong(), weeks.capture(), any());

        List<LocalDate> recalculated = weeks.getAllValues();
        // 11/03 ~ 11/28은 11/03, 11/10, 11/17, 11/24 네 개의 주에 걸쳐 있다.
        assertEquals(List.of(
                LocalDate.of(2025, 11, 3),
                LocalDate.of(2025, 11, 10),
                LocalDate.of(2025, 11, 17),
                LocalDate.of(2025, 11, 24)), recalculated,
                "예전에는 삭제 시작일이 속한 한 주만 재계산해 나머지 주 금액이 stale하게 남았다");
        assertTrue(recalculated.stream().allMatch(d -> d.getDayOfWeek() == DayOfWeek.MONDAY),
                "재계산 기준일은 항상 그 주의 월요일이어야 한다");
    }

    @Test
    @DisplayName("한 주 안에서만 삭제되면 한 번만 재계산한다")
    void 같은_주_삭제는_한_번만() {
        LocalDate deleteFrom = LocalDate.of(2025, 11, 4);   // 화요일
        LocalDate groupLast = LocalDate.of(2025, 11, 7);    // 같은 주 금요일

        givenWork(deleteFrom);
        when(workRepository.findLastWorkDateByRepeatGroupId(GROUP_ID)).thenReturn(Optional.of(groupLast));
        when(workRepository.deleteRecurringWorkFromDate(GROUP_ID, deleteFrom)).thenReturn(3);
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.empty());

        service().deleteRecurringWorkIncludingDate(REQUESTER_USER_ID, WORK_ID);

        ArgumentCaptor<LocalDate> weeks = ArgumentCaptor.forClass(LocalDate.class);
        verify(salaryCalculationService, atLeastOnce())
                .recalculateWorkWeek(anyLong(), weeks.capture(), any());
        assertEquals(List.of(LocalDate.of(2025, 11, 3)), weeks.getAllValues());
    }

    private void givenWork(LocalDate workDate) {
        Work work = Work.builder()
                .id(WORK_ID).workerId(WORKER_ID).workDate(workDate)
                .startTime(workDate.atTime(9, 0)).endTime(workDate.atTime(18, 0))
                .restTimeMinutes(60).hourlyRate(10_000).repeatGroupId(GROUP_ID)
                .build();
        Worker worker = Worker.builder()
                .id(WORKER_ID).userId(REQUESTER_USER_ID).workplaceId(20L).isAccepted(true).build();
        Workplace workplace = Workplace.builder()
                .id(20L).ownerId(2L).workplaceName("테스트 근무지").build();

        when(workRepository.findById(WORK_ID)).thenReturn(Optional.of(work));
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(20L)).thenReturn(Optional.of(workplace));
    }
}
