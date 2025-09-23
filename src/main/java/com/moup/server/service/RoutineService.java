package com.moup.server.service;

import com.moup.server.exception.RoutineAlreadyExistsException;
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
        Routine routine = routineCreateRequest.toEntity(userId);
        if (routineRepository.existsByUserIdAndRoutineName(userId, routine.getRoutineName())) { throw new RoutineAlreadyExistsException(); }
        routineRepository.create(routine);

        Routine createdRoutine = routineRepository.findByUserIdAndRoutineName(userId, routine.getRoutineName()).orElseThrow(RoutineNotFoundException::new);
        List<RoutineTaskCreateRequest> routineTaskCreateRequestList = routineCreateRequest.getRoutineTaskCreateRequestList();
        List<RoutineTask> tasksToCreate = routineTaskCreateRequestList.stream().map(request -> request.toEntity(createdRoutine.getId())).toList();

        routineTaskRepository.createTasks(tasksToCreate);

        List<RoutineTask> createdRoutineTask = routineTaskRepository.findAllByRoutineId(createdRoutine.getId());
        return RoutineCreateResponse.builder()
                .routineId(createdRoutine.getId())
                .taskIdList(createdRoutineTask.stream().map(RoutineTask::getId).toList())
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
    public RoutineUpdateResponse updateRoutine(Long userId, RoutineUpdateRequest routineUpdateRequest) {
        Routine routine = routineUpdateRequest.toEntity(userId);
        if (routineRepository.existsByUserIdAndRoutineName(userId, routine.getRoutineName())) { throw new RoutineAlreadyExistsException(); }
        routineRepository.update(routine);

        List<RoutineTask> existingTaskList = routineTaskRepository.findAllByRoutineId(routine.getId());
        List<RoutineTaskUpdateRequest> requestDtoList = routineUpdateRequest.getRoutineTaskUpdateRequestList();

        Map<Long, RoutineTask> existingTaskMap = existingTaskList.stream()
                .collect(Collectors.toMap(RoutineTask::getId, Function.identity()));

        List<RoutineTask> tasksToCreate = new ArrayList<>();
        List<RoutineTask> tasksToUpdate = new ArrayList<>();

        for (RoutineTaskUpdateRequest requestDto : requestDtoList) {
            Long taskId = requestDto.getTaskId();
            if (taskId == null) {
                // CREATE
                // 자동 생성된 ID가 불러와지는지 확인 필요
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

        if (!tasksToCreate.isEmpty()) {
            routineTaskRepository.createTasks(tasksToCreate);
        }
        if (!tasksToUpdate.isEmpty()) {
            routineTaskRepository.updateTasks(tasksToUpdate);
        }
        if (!idsToDelete.isEmpty()) {
            routineTaskRepository.deleteTasks(idsToDelete, routine.getId());
        }

        return RoutineUpdateResponse.builder()
                .routineId(routine.getId())
                .updatedTaskIdList(tasksToUpdate.stream().map(RoutineTask::getId).toList())
                .createdTaskIdList(tasksToCreate.stream().map(RoutineTask::getId).toList())
                .deletedTaskIdList(idsToDelete)
                .build();
    }

    @Transactional
    public RoutineDeleteResponse deleteRoutine(Long userId, Long routineId) {
        routineRepository.delete(routineId, userId);

        return RoutineDeleteResponse.builder()
                .routineId(routineId)
                .build();
    }
}
