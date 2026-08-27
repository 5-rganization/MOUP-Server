package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository.CompletionSnapshot;
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
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// Phase 10-1 — 근무별 할 일 체크 상태가 **반복 근무 교체에서 살아남는지**.
///
/// `replaceWithNewRecurringWorks`는 기존 근무를 지우고 새로 만든다. 완료 상태는
/// `work_id`를 FK로 잡으므로 그대로 두면 CASCADE로 전부 사라진다. 사용자가 한 일은
/// "근무 시간을 바꾼 것"뿐인데 **오늘 체크해 둔 항목이 조용히 전부 풀린다.**
///
/// 계획 문서가 이 지점을 미리 경고했다 — 완료 상태를 Phase 5(반복 근무)와 함께
/// 설계해야 한다고. 이 테스트가 그 경고를 코드로 고정한다.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RoutineTaskCompletionTest {

    private static final Long WORK_ID = 1L;
    private static final Long WORKER_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long OWNER_ID = 2L;
    private static final Long TASK_ID = 500L;
    private static final String GROUP_ID = "group-uuid";

    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoutineService routineService;
    @Mock private SalaryCalculationService salaryCalculationService;
    @Mock private RoutineTaskCompletionRepository completionRepository;

    private WorkService workService;

    /// 월요일. 이 날 근무를 편집한다.
    private static final LocalDate MONDAY = LocalDate.of(2025, 11, 10);

    @BeforeEach
    void setUp() {
        workService = new WorkService(workRepository, salaryRepository, workerRepository,
                workplaceRepository, userRepository, routineService,
                salaryCalculationService, new PermissionVerifyUtil(userRepository),
                completionRepository);

        Work current = Work.builder()
                .id(WORK_ID).workerId(WORKER_ID).workDate(MONDAY)
                .startTime(MONDAY.atTime(9, 0)).endTime(MONDAY.atTime(18, 0))
                .restTimeMinutes(60).hourlyRate(10_000).repeatGroupId(GROUP_ID)
                .build();

        when(workRepository.findById(WORK_ID)).thenReturn(Optional.of(current));
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(Worker.builder()
                .id(WORKER_ID).userId(USER_ID).workplaceId(20L).isAccepted(true).build()));
        when(workplaceRepository.findById(20L)).thenReturn(Optional.of(Workplace.builder()
                .id(20L).ownerId(OWNER_ID).workplaceName("편의점").build()));
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.empty());
        when(workRepository.findLastWorkDateByRepeatGroupId(GROUP_ID))
                .thenReturn(Optional.of(MONDAY.plusDays(7)));
        when(salaryCalculationService.calculateDailyIncome(any(), anyInt(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(0));

        // createBatch로 저장된 근무에 DB가 하듯 id를 붙이고, 재조회는 그것을 돌려준다.
        List<Work> saved = new ArrayList<>();
        doAnswer(inv -> {
            List<Work> works = inv.getArgument(0);
            long nextId = 1000L;
            for (Work w : works) {
                saved.add(w.toBuilder().id(nextId++).build());
            }
            return null;
        }).when(workRepository).createBatch(anyList());
        when(workRepository.findAllByWorkerIdAndDateRange(eq(WORKER_ID), any(), any()))
                .thenAnswer(inv -> new ArrayList<>(saved));
    }

    private MyWorkUpdateRequest request(List<DayOfWeek> repeatDays) {
        return MyWorkUpdateRequest.builder()
                .routineIdList(List.of())
                .startTime(MONDAY.atTime(10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant())
                .endTime(MONDAY.atTime(19, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant())
                .restTimeMinutes(60)
                .repeatDays(repeatDays)
                .repeatEndDate(MONDAY.plusDays(7))
                .build();
    }

    @Test
    @DisplayName("반복 교체 후 같은 날짜의 새 근무에 체크가 되살아난다")
    void 체크가_교체를_넘어_유지된다() {
        when(completionRepository.findSnapshotsByRepeatGroupFrom(GROUP_ID, MONDAY))
                .thenReturn(List.of(new CompletionSnapshot(MONDAY, TASK_ID)));

        workService.updateMyRecurringWork(USER_ID, WORK_ID, request(List.of(DayOfWeek.MONDAY)));

        // 새로 만들어진 월요일 근무(id 1000)에 체크가 다시 붙어야 한다.
        verify(completionRepository).complete(1000L, TASK_ID);
    }

    /// 스냅샷을 삭제 **뒤에** 뜨면 이미 CASCADE로 사라진 뒤라 항상 빈 리스트가 온다.
    /// 기능이 조용히 아무것도 하지 않게 되고, 테스트도 "complete가 안 불렸다"로만 보여
    /// 원인을 짚기 어렵다. 순서 자체를 고정한다.
    @Test
    @DisplayName("체크 스냅샷은 근무 삭제보다 먼저 일어난다")
    void 스냅샷이_삭제보다_먼저() {
        when(completionRepository.findSnapshotsByRepeatGroupFrom(GROUP_ID, MONDAY))
                .thenReturn(List.of(new CompletionSnapshot(MONDAY, TASK_ID)));

        workService.updateMyRecurringWork(USER_ID, WORK_ID, request(List.of(DayOfWeek.MONDAY)));

        InOrder inOrder = inOrder(completionRepository, workRepository);
        inOrder.verify(completionRepository).findSnapshotsByRepeatGroupFrom(GROUP_ID, MONDAY);
        inOrder.verify(workRepository).deleteRecurringWorkFromDate(GROUP_ID, MONDAY);
    }

    /// 새 반복이 그 요일을 더 이상 포함하지 않으면 그 날 근무 자체가 없어진다.
    /// 붙일 곳이 없으니 체크는 버려지는 것이 맞다 — 엉뚱한 날짜에 옮겨 붙이면 안 된다.
    @Test
    @DisplayName("새 반복에 없는 요일의 체크는 버려진다")
    void 사라진_요일의_체크는_버려진다() {
        when(completionRepository.findSnapshotsByRepeatGroupFrom(GROUP_ID, MONDAY))
                .thenReturn(List.of(new CompletionSnapshot(MONDAY, TASK_ID)));

        // 화요일 반복으로 바꾼다 — 월요일 근무는 더 이상 만들어지지 않는다.
        workService.updateMyRecurringWork(USER_ID, WORK_ID, request(List.of(DayOfWeek.TUESDAY)));

        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }

    @Test
    @DisplayName("체크가 없으면 복원 시도도 하지 않는다")
    void 체크가_없으면_아무것도_하지_않는다() {
        when(completionRepository.findSnapshotsByRepeatGroupFrom(GROUP_ID, MONDAY))
                .thenReturn(List.of());

        workService.updateMyRecurringWork(USER_ID, WORK_ID, request(List.of(DayOfWeek.MONDAY)));

        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }
}
