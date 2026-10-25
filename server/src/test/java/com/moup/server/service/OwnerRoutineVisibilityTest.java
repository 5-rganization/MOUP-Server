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
        when(routineTaskRepository.findAllByRoutineIdIn(List.of(7L))).thenReturn(List.of());

        List<RoutineSummaryResponse> routines = routineService.getAllRoutineByWorkRoutineMapping(WORKER_USER_ID, WORK_ID);

        assertEquals(LocalTime.of(8, 30), routines.get(0).getAlarmTime());
        assertNotNull(routines.get(0).getRoutineTaskList());
    }

    /// 겹치면 `UNIQUE (routine_id, order_index)`에 걸려 422가 아니라 500이 났다(스코프 6 M6).
    @Test
    @DisplayName("할 일의 정렬 순서가 겹치면 저장하기 전에 422로 거절한다")
    void 정렬_순서_중복() {
        var request = com.moup.domain.routine.dto.RoutineCreateRequest.builder()
                .routineName("오픈").alarmTime(LocalTime.of(8, 0))
                .routineTaskList(List.of(
                        com.moup.domain.routine.dto.RoutineTaskCreateRequest.builder().content("A").orderIndex(0).build(),
                        com.moup.domain.routine.dto.RoutineTaskCreateRequest.builder().content("B").orderIndex(0).build()))
                .build();

        assertThrows(com.moup.global.error.InvalidFieldFormatException.class,
                () -> routineService.createRoutine(WORKER_USER_ID, request));
        verify(routineRepository, never()).create(any());
    }

    // ================= 루틴 수정과 체크 기록 =================

    private void givenRoutineWithTasks() {
        when(routineRepository.findByIdAndUserId(7L, WORKER_USER_ID)).thenReturn(Optional.of(
                Routine.builder().id(7L).userId(WORKER_USER_ID).routineName("오픈").build()));
        when(routineTaskRepository.findAllByRoutineId(7L)).thenReturn(List.of(
                com.moup.domain.routine.domain.RoutineTask.builder().id(10L).routineId(7L).content("A").orderIndex(0).build(),
                com.moup.domain.routine.domain.RoutineTask.builder().id(11L).routineId(7L).content("B").orderIndex(1).build()));
    }

    private static com.moup.domain.routine.dto.RoutineUpdateRequest update(com.moup.domain.routine.dto.RoutineTaskUpdateRequest... tasks) {
        return com.moup.domain.routine.dto.RoutineUpdateRequest.builder()
                .routineName("오픈").alarmTime(LocalTime.of(8, 0)).routineTaskList(List.of(tasks)).build();
    }

    /// 예전에는 할 일을 전부 지우고 다시 만들어, 루틴을 고칠 때마다 그 할 일들의 오늘 체크가 CASCADE로 풀렸다.
    @Test
    @DisplayName("ID를 보낸 할 일은 지우지 않고 제자리에서 고친다 — 순서를 맞바꿔도 UNIQUE에 걸리지 않게 먼저 비켜 둔다")
    void 제자리_수정() {
        givenRoutineWithTasks();

        routineService.updateRoutine(WORKER_USER_ID, 7L, update(
                com.moup.domain.routine.dto.RoutineTaskUpdateRequest.builder().taskId(11L).content("B").orderIndex(0).build(),
                com.moup.domain.routine.dto.RoutineTaskUpdateRequest.builder().content("C").orderIndex(1).build()));

        var order = inOrder(routineTaskRepository);
        order.verify(routineTaskRepository).deleteAllByRoutineIdExcept(7L, List.of(11L));
        order.verify(routineTaskRepository).moveOrderIndexesOutOfTheWay(7L);
        order.verify(routineTaskRepository).updateContentAndOrder(11L, 7L, "B", 0);
        order.verify(routineTaskRepository).createBatch(argThat(list -> list.size() == 1 && list.get(0).getContent().equals("C")));
        verify(routineTaskRepository, never()).deleteAllByRoutineId(anyLong());
    }

    @Test
    @DisplayName("다른 루틴의 할 일 ID를 보내면 아무것도 바꾸지 않고 422")
    void 남의_할_일_ID() {
        givenRoutineWithTasks();

        assertThrows(com.moup.global.error.InvalidFieldFormatException.class, () -> routineService.updateRoutine(WORKER_USER_ID, 7L,
                update(com.moup.domain.routine.dto.RoutineTaskUpdateRequest.builder().taskId(999L).content("X").orderIndex(0).build())));
        verify(routineRepository, never()).update(any());
    }

    @Test
    @DisplayName("ID를 남기지 않으면(기존 앱) 할 일 삭제 SQL이 루틴 전체를 지운다")
    void 기존_앱_호환_SQL() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.addMapper(RoutineTaskRepository.class);
        var statement = configuration.getMappedStatement(RoutineTaskRepository.class.getName() + ".deleteAllByRoutineIdExcept");
        java.util.Map<String, Object> none = new java.util.HashMap<>();
        none.put("routineId", 7L);
        none.put("keepIds", List.of());

        assertFalse(statement.getBoundSql(none).getSql().contains("NOT IN"));
        assertTrue(statement.getBoundSql(java.util.Map.of("routineId", 7L, "keepIds", List.of(11L))).getSql().contains("NOT IN"));
    }

    /// 루틴마다 매핑·근무·요일을 따로 조회해 루틴 20개면 약 61쿼리였다(스코프 6 I4).
    @Test
    @DisplayName("루틴 목록은 루틴 수와 무관하게 연결 근무를 한 번에 읽는다")
    void 루틴_목록_N_플러스_1_없음() {
        when(routineRepository.findAllByUserId(WORKER_USER_ID)).thenReturn(List.of(
                Routine.builder().id(1L).routineName("A").build(),
                Routine.builder().id(2L).routineName("B").build(),
                Routine.builder().id(3L).routineName("C").build()));
        when(workRoutineMappingRepository.findAllByRoutineIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                WorkRoutineMapping.builder().workId(50L).routineId(1L).build(),
                WorkRoutineMapping.builder().workId(51L).routineId(3L).build()));
        when(workRepository.findAllByIdIn(List.of(50L, 51L))).thenReturn(List.of(
                Work.builder().id(50L).repeatGroupId("g").build(), Work.builder().id(51L).build()));
        when(workRepository.findDayNamesByGroupIdsIn(List.of("g"))).thenReturn(List.of(
                new WorkRepository.GroupIdAndDayName("g", "Monday"), new WorkRepository.GroupIdAndDayName("g", null)));

        var routines = routineService.getAllRoutine(WORKER_USER_ID).getRoutineSummaryInfoList();

        verify(workRoutineMappingRepository, times(1)).findAllByRoutineIdIn(any());
        verify(workRepository, times(1)).findAllByIdIn(any());
        assertEquals(List.of(java.time.DayOfWeek.MONDAY), routines.get(0).getLinkedWorks().get(0).repeatDays());
        assertEquals(List.of(), routines.get(1).getLinkedWorks());
        assertEquals(List.of(), routines.get(2).getLinkedWorks().get(0).repeatDays(), "단일 근무는 반복 요일이 없다");
    }

    /// 예전에는 반복 근무마다 DELETE·SELECT·INSERT를 따로 해 365일 반복이면 약 1,000쿼리였다(스코프 2 I9).
    @Test
    @DisplayName("새 반복 근무에 루틴을 연결할 때 검증 1번, INSERT 1번으로 끝낸다")
    void 반복_근무_루틴_일괄_연결() {
        when(routineRepository.findAllByIdListInAndUserId(List.of(7L, 8L), WORKER_USER_ID)).thenReturn(List.of(
                Routine.builder().id(7L).build(), Routine.builder().id(8L).build()));

        routineService.linkRoutinesToNewWorks(WORKER_USER_ID, List.of(7L, 8L), List.of(1L, 2L, 3L));

        verify(workRoutineMappingRepository, times(1)).createBatch(argThat(list -> list.size() == 6));
        verify(workRoutineMappingRepository, never()).deleteByWorkId(anyLong());
    }

    @Test
    @DisplayName("남의 루틴이 섞이면 아무것도 연결하지 않는다")
    void 남의_루틴_연결_불가() {
        when(routineRepository.findAllByIdListInAndUserId(List.of(7L, 99L), WORKER_USER_ID)).thenReturn(List.of(
                Routine.builder().id(7L).build()));

        assertThrows(com.moup.domain.routine.exception.RoutineNotFoundException.class,
                () -> routineService.linkRoutinesToNewWorks(WORKER_USER_ID, List.of(7L, 99L), List.of(1L)));
        verify(workRoutineMappingRepository, never()).createBatch(any());
    }
}
