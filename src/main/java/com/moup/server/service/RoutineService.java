package com.moup.server.service;

import com.moup.server.exception.RoutineNameAlreadyUsedException;
import com.moup.server.exception.RoutineNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Routine;
import com.moup.server.model.entity.RoutineTask;
import com.moup.server.repository.RoutineRepository;
import com.moup.server.repository.RoutineTaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final RoutineTaskRepository routineTaskRepository;

    @Transactional
    public RoutineCreateResponse createRoutine(Long userId, RoutineCreateRequest request) {
        if (routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) { throw new RoutineNameAlreadyUsedException(); }

        Routine routineToCreate = request.toEntity(userId);
        routineRepository.create(routineToCreate);

        List<RoutineTaskCreateRequest> routineTaskCreateRequestList = request.getRoutineTaskList();
        List<RoutineTask> tasksToCreate = routineTaskCreateRequestList.stream()
                .map(taskCreateRequest -> taskCreateRequest.toEntity(routineToCreate.getId()))
                .toList();

        for (RoutineTask task : tasksToCreate) { routineTaskRepository.create(task); }

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
                .alarmTime(routine.getAlarmTime() != null ? routine.getAlarmTime().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse getAllSummarizedRoutine(Long userId) {
        List<Routine> routineList = routineRepository.findAllByUserId(userId);
        List<RoutineSummaryResponse> routineSummaryResponseList = routineList.stream()
                .map(routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime() != null ? routine.getAlarmTime().toString() : null)
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
                        .isChecked(task.getIsChecked())
                        .build())
                .toList();

        return RoutineDetailResponse.builder()
                .routineId(routine.getId())
                .routineName(routine.getRoutineName())
                .alarmTime(routine.getAlarmTime() != null ? routine.getAlarmTime().toString() : null)
                .routineTaskList(routineTaskDetailResponseList)
                .build();
    }

    @Transactional
    public void updateRoutine(Long userId, Long routineId, RoutineUpdateRequest request) {
        Routine oldRoutine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
        if (!oldRoutine.getRoutineName().equals(request.getRoutineName())
                && routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) { throw new RoutineNameAlreadyUsedException(); }

        Routine newRoutine = request.toEntity(routineId, userId);
        routineRepository.update(newRoutine);

        List<RoutineTask> existingTaskList = routineTaskRepository.findAllByRoutineId(newRoutine.getId());
        List<RoutineTaskUpdateRequest> requestDtoList = request.getRoutineTaskList();
        boolean isStructureChanged = false;
        if (existingTaskList.size() != requestDtoList.size()) {
            isStructureChanged = true;
        } else {
            Map<Long, RoutineTask> existingTaskMap = existingTaskList.stream()
                    .collect(Collectors.toMap(RoutineTask::getId, Function.identity()));

            for (RoutineTaskUpdateRequest dto : requestDtoList) {
                if (dto.getTaskId() == null
                        || !existingTaskMap.containsKey(dto.getTaskId())
                        || !existingTaskMap.get(dto.getTaskId()).getOrderIndex().equals(dto.getOrderIndex())) {
                    isStructureChanged = true;
                    break;
                }
            }
        }

        if (isStructureChanged) {
            routineTaskRepository.deleteAllByRoutineId(newRoutine.getId());
            if (!requestDtoList.isEmpty()) {
                List<RoutineTask> tasksToCreate = requestDtoList.stream()
                        .map(taskUpdateRequest -> taskUpdateRequest.toEntity(routineId))
                        .toList();
                for (RoutineTask task : tasksToCreate) { routineTaskRepository.create(task); }
            }
        } else {
            List<RoutineTask> tasksToUpdate = requestDtoList.stream()
                    .map(taskUpdateRequest -> taskUpdateRequest.toEntity(routineId))
                    .toList();
            for (RoutineTask task : tasksToUpdate) { routineTaskRepository.update(task); }
        }
    }

    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        if (routineRepository.existByIdAndUserId(routineId, userId)) {
            routineRepository.deleteByIdAndUserId(routineId, userId);
        } else {
            throw new RoutineNotFoundException();
        }
    }
}
