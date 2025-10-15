package com.moup.server.service;

import com.moup.server.exception.DataLimitExceedException;
import com.moup.server.exception.RoutineNameAlreadyUsedException;
import com.moup.server.exception.RoutineNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Routine;
import com.moup.server.model.entity.RoutineTask;
import com.moup.server.model.entity.WorkRoutineMapping;
import com.moup.server.repository.RoutineRepository;
import com.moup.server.repository.RoutineTaskRepository;
import com.moup.server.repository.WorkRoutineMappingRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final RoutineTaskRepository routineTaskRepository;
    private final WorkRoutineMappingRepository workRoutineMappingRepository;

    private static final int MAX_ROUTINE_COUNT_PER_USER = 20; // 사용자당 루틴 연결 최대 개수
    private static final int MAX_TASK_COUNT_PER_ROUTINE = 50; // 루틴당 할 일 연결 최대 개수
    private static final int MAX_ROUTINE_COUNT_PER_WORK = 10; // 근무당 루틴 연결 최대 개수

    @Transactional
    public RoutineCreateResponse createRoutine(Long userId, RoutineCreateRequest request) {
        if (routineRepository.findAllByUserId(userId).size() > MAX_ROUTINE_COUNT_PER_USER) {
            throw new DataLimitExceedException("루틴은 사용자당 최대 " + MAX_ROUTINE_COUNT_PER_USER + "개까지 생성할 수 있습니다.");
        }
        if (routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) { throw new RoutineNameAlreadyUsedException(); }

        List<RoutineTaskCreateRequest> routineTaskCreateRequestList = request.getRoutineTaskList();
        if (routineTaskCreateRequestList.size() > MAX_TASK_COUNT_PER_ROUTINE) {
            throw new DataLimitExceedException("할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
        }

        Routine routineToCreate = request.toEntity(userId);
        routineRepository.create(routineToCreate);

        List<RoutineTask> taskListToCreate = routineTaskCreateRequestList.stream()
                .map(taskCreateRequest -> taskCreateRequest.toEntity(routineToCreate.getId()))
                .toList();

        for (RoutineTask task : taskListToCreate) { routineTaskRepository.create(task); }

        return RoutineCreateResponse.builder()
                .routineId(routineToCreate.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryResponse getSummarizedRoutine(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);

        return RoutineSummaryResponse.builder()
                .routineId(routine.getId())
                .routineName(routine.getRoutineName())
                .alarmTime(routine.getAlarmTime())
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse getAllSummarizedRoutine(Long userId) {
        List<Routine> routineList = routineRepository.findAllByUserId(userId);
        List<RoutineSummaryResponse> routineSummaryResponseList = routineList.stream()
                .map(routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime())
                        .build())
                .toList();

        return RoutineSummaryListResponse.builder()
                .routineSummaryList(routineSummaryResponseList)
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse getRoutineDetail(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
        List<RoutineTask> routineTaskList = routineTaskRepository.findAllByRoutineId(routineId);
        List<RoutineTaskDetailResponse> routineTaskDetailResponseList = routineTaskList.stream()
                .map(task -> RoutineTaskDetailResponse.builder()
                        .taskId(task.getId())
                        .routineId(task.getRoutineId())
                        .content(task.getContent())
                        .orderIndex(task.getOrderIndex())
                        .build())
                .toList();

        return RoutineDetailResponse.builder()
                .routineId(routine.getId())
                .routineName(routine.getRoutineName())
                .alarmTime(routine.getAlarmTime())
                .routineTaskList(routineTaskDetailResponseList)
                .build();
    }

    @Transactional
    public void updateRoutine(Long userId, Long routineId, RoutineUpdateRequest request) {
        Routine oldRoutine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
        if (!oldRoutine.getRoutineName().equals(request.getRoutineName())
                && routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) { throw new RoutineNameAlreadyUsedException(); }

        List<RoutineTaskUpdateRequest> routineTaskUpdateRequestList = request.getRoutineTaskList();
        if (routineTaskUpdateRequestList.size() > MAX_TASK_COUNT_PER_ROUTINE) {
            throw new DataLimitExceedException("할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
        }

        Routine newRoutine = request.toEntity(routineId, userId);
        routineRepository.update(newRoutine);

        routineTaskRepository.deleteAllByRoutineId(routineId);

        List<RoutineTask> taskListToCreate = routineTaskUpdateRequestList.stream()
                .map(routineTaskUpdateRequest -> routineTaskUpdateRequest.toEntity(routineId))
                .toList();

        for (RoutineTask task : taskListToCreate) { routineTaskRepository.create(task); }
    }

    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        if (routineRepository.existByIdAndUserId(routineId, userId)) {
            routineTaskRepository.delete(userId, routineId);
            routineRepository.delete(routineId, userId);
        } else {
            throw new RoutineNotFoundException();
        }
    }

    @Transactional
    public void saveWorkRoutineMapping(Long userId, List<Long> routineIdList, Long workId) {
        if (routineIdList.size() > MAX_ROUTINE_COUNT_PER_WORK) {
            throw new DataLimitExceedException("루틴은 한 근무당 최대 " + MAX_ROUTINE_COUNT_PER_WORK + "개까지 연결할 수 있습니다.");
        }

        workRoutineMappingRepository.delete(workId);

        for (Long routineId : routineIdList) {
            if (!routineRepository.existByIdAndUserId(routineId, userId)) { throw new RoutineNotFoundException(); }
            WorkRoutineMapping workRoutineMapping = WorkRoutineMapping.builder()
                    .workId(workId)
                    .routineId(routineId)
                    .build();
            workRoutineMappingRepository.create(workRoutineMapping);
        }
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> getAllSummarizedRoutineByWorkRoutineMapping(Long userId, Long workId) {
        List<WorkRoutineMapping> workRoutineMappingList = workRoutineMappingRepository.findAllByWorkId(workId);
        return workRoutineMappingList.stream()
                .map(mapping -> routineRepository.findByIdAndUserId(mapping.getRoutineId(), userId)
                        .orElseThrow(RoutineNotFoundException::new))
                .map(routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteWorkRoutineMapping(Long workId) {
        workRoutineMappingRepository.delete(workId);
    }
}
