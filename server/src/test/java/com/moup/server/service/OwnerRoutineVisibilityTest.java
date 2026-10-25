package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.domain.Routine;
import com.moup.domain.routine.dto.RoutineSummaryResponse;
import com.moup.domain.routine.mapper.RoutineRepository;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.routine.mapper.RoutineTaskRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.domain.WorkRoutineMapping;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.work.mapper.WorkRoutineMappingRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 확정 정책 14 — 사장님은 알바생 근무에 연결된 루틴의 **제목만** 본다 (스코프 6 I1).
@ExtendWith(MockitoExtension.class)
class OwnerRoutineVisibilityTest {

    private static final Long OWNER_ID = 1L;
    private static final Long WORKER_USER_ID = 2L;
    private static final Long WORK_ID = 50L;

    @Mock private RoutineRepository routineRepository;
    @Mock private PermissionVerifyUtil permissionVerifyUtil;
    @Mock private RoutineTaskRepository routineTaskRepository;
    @Mock private WorkRoutineMappingRepository workRoutineMappingRepository;
    @Mock private WorkRepository workRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private RoutineTaskCompletionRepository routineTaskCompletionRepository;

    @InjectMocks private RoutineService routineService;

    private void givenWorkWithRoutine() {
        when(workRepository.findById(WORK_ID)).thenReturn(Optional.of(Work.builder().id(WORK_ID).workerId(100L).build()));
        when(workerRepository.findById(100L)).thenReturn(Optional.of(
                Worker.builder().id(100L).userId(WORKER_USER_ID).workplaceId(10L).isAccepted(true).build()));
        when(workplaceRepository.findById(10L)).thenReturn(Optional.of(Workplace.builder().id(10L).ownerId(OWNER_ID).build()));
        when(workRoutineMappingRepository.findAllByWorkId(WORK_ID))
                .thenReturn(List.of(WorkRoutineMapping.builder().workId(WORK_ID).routineId(7L).build()));
        when(routineRepository.findAllByIdListInAndUserId(List.of(7L), WORKER_USER_ID)).thenReturn(List.of(
                Routine.builder().id(7L).userId(WORKER_USER_ID).routineName("오픈 루틴").alarmTime(LocalTime.of(8, 30)).build()));
    }

    /// 예전에는 루틴을 요청자(사장님) 소유로만 찾아 **항상 빈 배열**이었다.
    @Test
    @DisplayName("사장님에게는 알바생 루틴의 제목만 보이고 할 일·알람·다른 근무는 빠진다")
    void 사장님은_제목만() {
        givenWorkWithRoutine();

        List<RoutineSummaryResponse> routines = routineService.getAllRoutineByWorkRoutineMapping(OWNER_ID, WORK_ID);

        assertEquals(1, routines.size());
        RoutineSummaryResponse routine = routines.get(0);
        assertAll(
                () -> assertEquals("오픈 루틴", routine.getRoutineName()),
                () -> assertNull(routine.getAlarmTime()),
                () -> assertNull(routine.getRoutineTaskList()),
                // linkedWorks에는 알바생의 다른 근무지 근무까지 들어 있다
                () -> assertNull(routine.getLinkedWorks())
        );
        verifyNoInteractions(routineTaskRepository, routineTaskCompletionRepository);
    }

    @Test
    @DisplayName("알바생 본인은 할 일과 완료 여부까지 본다")
    void 알바생은_전체() {
        givenWorkWithRoutine();
        when(routineTaskCompletionRepository.findCompletedTaskIdsByWorkId(WORK_ID)).thenReturn(List.of());
        when(routineTaskRepository.findAllByRoutineId(7L)).thenReturn(List.of());

        List<RoutineSummaryResponse> routines = routineService.getAllRoutineByWorkRoutineMapping(WORKER_USER_ID, WORK_ID);

        assertEquals(LocalTime.of(8, 30), routines.get(0).getAlarmTime());
        assertNotNull(routines.get(0).getRoutineTaskList());
    }
}
