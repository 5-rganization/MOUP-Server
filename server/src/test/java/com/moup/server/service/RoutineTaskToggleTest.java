package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.domain.RoutineTask;
import com.moup.domain.routine.mapper.RoutineRepository;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.routine.mapper.RoutineTaskRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.domain.WorkRoutineMapping;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.work.mapper.WorkRoutineMappingRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Phase 10-1 — 근무별 할 일 체크 토글의 권한과 유효성.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RoutineTaskToggleTest {

    private static final Long WORK_ID = 1L;
    private static final Long WORKER_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long OWNER_ID = 2L;
    private static final Long LINKED_ROUTINE_ID = 300L;
    private static final Long TASK_ID = 500L;

    @Mock private RoutineRepository routineRepository;
    @Mock private RoutineTaskRepository routineTaskRepository;
    @Mock private WorkRoutineMappingRepository workRoutineMappingRepository;
    @Mock private WorkRepository workRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private RoutineTaskCompletionRepository completionRepository;
    @Mock private UserRepository userRepository;

    private RoutineService routineService;

    @BeforeEach
    void setUp() {
        routineService = new RoutineService(routineRepository,
                new PermissionVerifyUtil(userRepository), routineTaskRepository,
                workRoutineMappingRepository, workRepository, workerRepository,
                workplaceRepository, completionRepository);

        when(workRepository.findById(WORK_ID)).thenReturn(Optional.of(Work.builder()
                .id(WORK_ID).workerId(WORKER_ID).workDate(LocalDate.of(2025, 11, 10)).build()));
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(Worker.builder()
                .id(WORKER_ID).userId(USER_ID).workplaceId(20L).isAccepted(true).build()));
        when(workplaceRepository.findById(20L)).thenReturn(Optional.of(Workplace.builder()
                .id(20L).ownerId(OWNER_ID).build()));
        when(workRoutineMappingRepository.findAllByWorkId(WORK_ID)).thenReturn(List.of(
                WorkRoutineMapping.builder().workId(WORK_ID).routineId(LINKED_ROUTINE_ID).build()));
    }

    private void givenTaskOfRoutine(Long routineId) {
        when(routineTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(
                RoutineTask.builder().id(TASK_ID).routineId(routineId).content("바닥 청소").build()));
    }

    @Test
    @DisplayName("완료로 표시한다")
    void 체크() {
        givenTaskOfRoutine(LINKED_ROUTINE_ID);

        routineService.setRoutineTaskCompletion(USER_ID, WORK_ID, TASK_ID, true);

        verify(completionRepository).complete(WORK_ID, TASK_ID);
        verify(completionRepository, never()).uncomplete(anyLong(), anyLong());
    }

    @Test
    @DisplayName("완료를 해제한다")
    void 체크_해제() {
        givenTaskOfRoutine(LINKED_ROUTINE_ID);

        routineService.setRoutineTaskCompletion(USER_ID, WORK_ID, TASK_ID, false);

        verify(completionRepository).uncomplete(WORK_ID, TASK_ID);
        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }

    /// FK는 "그 할 일이 존재하는가"만 보장한다. 남의 루틴 할 일 id를 넣는 것은 막지 못한다.
    /// 막지 않으면 자기 근무에 임의의 할 일 완료 기록을 심을 수 있고,
    /// 그 근무의 루틴 조회에는 뜨지 않아 눈에 보이지도 않는 쓰레기 행이 쌓인다.
    @Test
    @DisplayName("이 근무에 연결되지 않은 루틴의 할 일은 체크할 수 없다")
    void 연결되지_않은_할일은_거부() {
        givenTaskOfRoutine(999L);   // 이 근무에 연결되지 않은 루틴

        assertThrows(InvalidPermissionAccessException.class,
                () -> routineService.setRoutineTaskCompletion(USER_ID, WORK_ID, TASK_ID, true));
        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }

    /// 체크도 근무 기록이다 — 확정 정책 5의 "쓰기 차단"에 포함된다.
    @Test
    @DisplayName("사장님이 탈퇴한 근무지에서는 체크할 수 없다")
    void 사장님_탈퇴시_체크_불가() {
        givenTaskOfRoutine(LINKED_ROUTINE_ID);
        when(userRepository.isWithdrawn(OWNER_ID)).thenReturn(true);

        assertThrows(InvalidPermissionAccessException.class,
                () -> routineService.setRoutineTaskCompletion(USER_ID, WORK_ID, TASK_ID, true));
        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }

    /// 승인 대기 중인 근무자는 근무 행위를 할 수 없다 (확정 정책 6 (c) · 15).
    @Test
    @DisplayName("승인 대기 중인 근무자는 체크할 수 없다")
    void 미승인_근무자는_체크_불가() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(Worker.builder()
                .id(WORKER_ID).userId(USER_ID).workplaceId(20L).isAccepted(false).build()));
        givenTaskOfRoutine(LINKED_ROUTINE_ID);

        assertThrows(InvalidPermissionAccessException.class,
                () -> routineService.setRoutineTaskCompletion(USER_ID, WORK_ID, TASK_ID, true));
        verify(completionRepository, never()).complete(anyLong(), anyLong());
    }
}
