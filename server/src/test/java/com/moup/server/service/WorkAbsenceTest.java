package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.dto.MyWorkUpdateRequest;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.BadRequestException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// 결근 설정 API와, 결근이 다른 쓰기 경로에서 깨지지 않는지 (스코프 3 Q3).
@ExtendWith(MockitoExtension.class)
class WorkAbsenceTest {

    private static final Long USER_ID = 1L;
    private static final Long WORKER_ID = 10L;
    private static final Long WORK_ID = 100L;
    private static final String GROUP_ID = "group-1";
    /// 수요일
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 10, 7);

    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoutineService routineService;
    @Mock private SalaryCalculationService salaryCalculationService;
    @Mock private RoutineTaskCompletionRepository routineTaskCompletionRepository;

    private WorkService service() {
        return new WorkService(workRepository, salaryRepository, workerRepository,
                workplaceRepository, userRepository, routineService,
                salaryCalculationService, new PermissionVerifyUtil(userRepository), routineTaskCompletionRepository);
    }

    @Test
    @DisplayName("결근 처리하면 플래그를 저장하고, 그 근무를 결근 상태로 계산한 뒤 그 주를 재계산한다")
    void 결근_처리() {
        givenWork(baseWork().build());
        when(salaryCalculationService.calculateDailyIncome(any(), eq(0), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().setWorkAbsence(USER_ID, WORK_ID, true);

        ArgumentCaptor<Work> calculated = ArgumentCaptor.forClass(Work.class);
        InOrder order = inOrder(workRepository, salaryCalculationService);
        order.verify(workRepository).updateIsAbsentById(WORK_ID, true);
        order.verify(salaryCalculationService).calculateDailyIncome(calculated.capture(), eq(0), anyBoolean());
        order.verify(workRepository).updateWorkWeekDetailsBatch(any());
        order.verify(salaryCalculationService).recalculateWorkWeek(eq(WORKER_ID), eq(WORK_DATE), any());
        // 고정급은 주 재계산이 조기 반환한다. 이 근무가 결근 상태로 계산되지 않으면 근무시간이 남는다.
        assertTrue(calculated.getValue().getIsAbsent(), "결근 상태를 넘겨 계산해야 한다");
    }

    @Test
    @DisplayName("출근 기록이 있는 근무는 결근 처리할 수 없다")
    void 출근_기록이_있으면_거부() {
        givenWork(baseWork().actualStartTime(WORK_DATE.atTime(9, 3)).build());

        assertThrows(BadRequestException.class, () -> service().setWorkAbsence(USER_ID, WORK_ID, true));
        verify(workRepository, never()).updateIsAbsentById(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("출근 기록이 있어도 결근 해제는 막지 않는다")
    void 결근_해제는_허용() {
        givenWork(baseWork().actualStartTime(WORK_DATE.atTime(9, 3)).isAbsent(true).build());
        when(salaryCalculationService.calculateDailyIncome(any(), eq(0), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().setWorkAbsence(USER_ID, WORK_ID, false);

        verify(workRepository).updateIsAbsentById(WORK_ID, false);
    }

    @Test
    @DisplayName("결근한 근무에 출퇴근 기록을 남기는 수정은 거부한다")
    void 결근한_근무에_출근_기록_수정_거부() {
        givenWork(baseWork().isAbsent(true).build());

        MyWorkUpdateRequest request = updateRequest()
                .actualStartTime(WORK_DATE.atTime(9, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .build();

        assertThrows(BadRequestException.class, () -> service().updateMySingleWork(USER_ID, WORK_ID, request));
        verify(workRepository, never()).update(any());
    }

    @Test
    @DisplayName("결근한 근무의 예정 시간을 고쳐도 결근 상태로 계산된다")
    void 결근한_근무의_시간_수정() {
        givenWork(baseWork().isAbsent(true).build());
        when(salaryCalculationService.calculateDailyIncome(any(), eq(0), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().updateMySingleWork(USER_ID, WORK_ID, updateRequest().build());

        ArgumentCaptor<Work> calculated = ArgumentCaptor.forClass(Work.class);
        verify(salaryCalculationService).calculateDailyIncome(calculated.capture(), eq(0), anyBoolean());
        // 고정급은 주 재계산이 조기 반환해 여기서 계산한 값이 그대로 저장된다.
        assertTrue(calculated.getValue().getIsAbsent(), "결근을 빼고 계산하면 일하지 않은 시간이 기록된다");
    }

    @Test
    @DisplayName("반복 근무를 교체해도 같은 날짜의 결근은 유지되고, 그 주가 결근 상태로 다시 계산된다")
    void 반복_교체가_결근을_보존한다() {
        givenWork(baseWork().repeatGroupId(GROUP_ID).isAbsent(true).build());
        when(workRepository.findAbsentWorkDatesByRepeatGroupFrom(GROUP_ID, WORK_DATE)).thenReturn(List.of(WORK_DATE));
        when(salaryCalculationService.calculateDailyIncome(any(), eq(0), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // createBatch로 들어간 근무에 id를 붙여 재조회 결과로 돌려준다.
        AtomicReference<List<Work>> inserted = new AtomicReference<>(List.of());
        doAnswer(invocation -> {
            List<Work> works = invocation.getArgument(0);
            List<Work> withIds = new ArrayList<>();
            for (int i = 0; i < works.size(); i++) {
                withIds.add(works.get(i).toBuilder().id(500L + i).build());
            }
            inserted.set(withIds);
            return (long) works.size();
        }).when(workRepository).createBatch(any());
        when(workRepository.findAllByWorkerIdAndDateRange(eq(WORKER_ID), any(), any()))
                .thenAnswer(invocation -> inserted.get());

        // 수요일 시작, 수·금 반복, 같은 주 금요일까지 → 수(결근이던 날)·금 두 건
        MyWorkUpdateRequest request = updateRequest()
                .startTime(WORK_DATE.atTime(10, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(WORK_DATE.atTime(16, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .repeatDays(List.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .repeatEndDate(WORK_DATE.plusDays(2))
                .build();

        service().updateMyRecurringWork(USER_ID, WORK_ID, request);

        Work recreatedWednesday = inserted.get().stream()
                .filter(w -> w.getWorkDate().equals(WORK_DATE)).findFirst().orElseThrow();
        Work recreatedFriday = inserted.get().stream()
                .filter(w -> w.getWorkDate().equals(WORK_DATE.plusDays(2))).findFirst().orElseThrow();

        InOrder order = inOrder(workRepository, salaryCalculationService);
        order.verify(workRepository).createBatch(any());
        order.verify(workRepository).updateIsAbsentById(recreatedWednesday.getId(), true);
        // 생성 직후의 재계산은 결근 전 상태로 돌았다. 결근을 붙인 뒤 그 주를 한 번 더 해야 한다.
        // (교체 범위 재계산이 같은 주를 한 번 더 돌 수 있으므로 횟수는 묻지 않는다.)
        order.verify(salaryCalculationService, atLeastOnce())
                .recalculateWorkWeek(eq(WORKER_ID), eq(WORK_DATE.with(DayOfWeek.MONDAY)), any());
        verify(workRepository, never()).updateIsAbsentById(recreatedFriday.getId(), true);
    }

    // ================= 헬퍼 =================

    private static Work.WorkBuilder baseWork() {
        LocalDateTime start = WORK_DATE.atTime(9, 0);
        return Work.builder()
                .id(WORK_ID).workerId(WORKER_ID).workDate(WORK_DATE)
                .startTime(start).endTime(start.plusHours(8))
                .restTimeMinutes(0).hourlyRate(10_000)
                .hasNightAllowance(false).hasHolidayAllowance(true)
                .isAbsent(false);
    }

    private static MyWorkUpdateRequest.MyWorkUpdateRequestBuilder updateRequest() {
        return MyWorkUpdateRequest.builder()
                .routineIdList(List.of())
                .startTime(WORK_DATE.atTime(9, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(WORK_DATE.atTime(17, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .restTimeMinutes(0)
                .repeatDays(List.of());
    }

    private void givenWork(Work work) {
        Worker worker = Worker.builder()
                .id(WORKER_ID).userId(USER_ID).workplaceId(20L).isAccepted(true).build();
        Workplace workplace = Workplace.builder()
                .id(20L).ownerId(2L).workplaceName("테스트 근무지").build();

        when(workRepository.findById(WORK_ID)).thenReturn(Optional.of(work));
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(20L)).thenReturn(Optional.of(workplace));
    }
}
