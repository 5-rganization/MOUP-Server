package com.moup.server.service;

import com.moup.server.exception.RoutineNotFoundException;
import com.moup.server.exception.RoutineTaskNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Routine;
import com.moup.server.model.entity.RoutineTask;
import com.moup.server.repository.RoutineRepository;
import com.moup.server.repository.RoutineTaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public RoutineCreateResponse createRoutine(Long userId, RoutineCreateRequest routineCreateRequest) {
        Routine routineToCreate = routineCreateRequest.toEntity(userId);
        routineRepository.create(routineToCreate);

        List<RoutineTaskCreateRequest> routineTaskCreateRequestList = routineCreateRequest.getRoutineTaskCreateRequestList();
        List<RoutineTask> tasksToCreate = routineTaskCreateRequestList.stream().map(request -> request.toEntity(routineToCreate.getId())).toList();
        routineTaskRepository.createTasks(tasksToCreate);

        return RoutineCreateResponse.builder()
                .routineId(routineToCreate.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse summarizeAllRoutine(Long userId) {
        List<Routine> routineList = routineRepository.findAllByUserId(userId);
        List<RoutineSummaryResponse> routineSummaryResponseList = routineList.stream().map(
                routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime() != null ? routine.getAlarmTime().toString() : null)
                        .build()
        ).toList();

        return RoutineSummaryListResponse.builder()
                .routineSummaryResponseList(routineSummaryResponseList)
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse findRoutineDetail(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
        List<RoutineTask> routineTaskList = routineTaskRepository.findAllByRoutineId(routineId);
        List<RoutineTaskDetailResponse> routineTaskDetailResponseList = routineTaskList.stream().map(
                task -> RoutineTaskDetailResponse.builder()
                        .taskId(task.getId())
                        .routineId(task.getRoutineId())
                        .content(task.getContent())
                        .orderIndex(task.getOrderIndex())
                        .isChecked(task.isChecked())
                        .build()
        ).toList();

        return RoutineDetailResponse.builder()
                .routineId(routine.getId())
                .routineName(routine.getRoutineName())
                .alarmTime(routine.getAlarmTime().toString())
                .routineTaskDetailResponseList(routineTaskDetailResponseList)
                .build();
    }

    @Transactional
    public void updateRoutine(Long userId, RoutineUpdateRequest routineUpdateRequest) {
        Routine newRoutine = routineUpdateRequest.toEntity(userId);
        if (routineRepository.existsByIdAndUserId(newRoutine.getId(), userId)) {
            routineRepository.update(newRoutine);
        } else {
            throw new RoutineNotFoundException();
        }

        List<RoutineTask> existingTaskList = routineTaskRepository.findAllByRoutineId(newRoutine.getId());
        List<RoutineTaskUpdateRequest> requestDtoList = routineUpdateRequest.getRoutineTaskUpdateRequestList();

        Map<Long, RoutineTask> existingTaskMap = existingTaskList.stream()
                .collect(Collectors.toMap(RoutineTask::getId, Function.identity()));

        List<RoutineTask> tasksToCreate = new ArrayList<>();
        List<RoutineTask> tasksToUpdate = new ArrayList<>();

        for (RoutineTaskUpdateRequest requestDto : requestDtoList) {
            Long taskId = requestDto.getTaskId();
            if (taskId == null) {
                // CREATE
                tasksToCreate.add(requestDto.toEntity());
            } else {
                if (existingTaskMap.containsKey(taskId)) {
                    // UPDATE
                    tasksToUpdate.add(requestDto.toEntity());
                    existingTaskMap.remove(taskId);
                } else {
                    throw new RoutineTaskNotFoundException();
                }
            }
        }
        // DELETE
        List<Long> idsToDelete = new ArrayList<>(existingTaskMap.keySet());

        // DB 작업 수행
        if (!tasksToCreate.isEmpty()) {
            routineTaskRepository.createTasks(tasksToCreate);
        }
        if (!tasksToUpdate.isEmpty()) {
            routineTaskRepository.updateTasks(tasksToUpdate);
        }
        if (!idsToDelete.isEmpty()) {
            routineTaskRepository.deleteTasks(idsToDelete, newRoutine.getId());
        }
    }

    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        if (routineRepository.existsByIdAndUserId(routineId, userId)) {
            routineRepository.deleteByIdAndUserId(routineId, userId);
        } else {
            throw new RoutineNotFoundException();
        }
    }
}
