package com.moup.server.service;

import com.moup.server.exception.DataLimitExceedException;
import com.moup.server.exception.RoutineNameAlreadyUsedException;
import com.moup.server.exception.RoutineNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    private final WorkRepository workRepository;
    private final WorkerRepository workerRepository;

    @Transactional
    public RoutineCreateResponse createRoutine(Long userId, RoutineCreateRequest request) {
        if (routineRepository.countByUserId(userId) >= MAX_ROUTINE_COUNT_PER_USER) {
            throw new DataLimitExceedException("루틴은 사용자당 최대 " + MAX_ROUTINE_COUNT_PER_USER + "개까지 생성할 수 있습니다.");
        }
        if (routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) { throw new RoutineNameAlreadyUsedException(); }

        List<RoutineTaskCreateRequest> routineTaskCreateRequestList = request.getRoutineTaskList();
        if (routineTaskCreateRequestList.size() >= MAX_TASK_COUNT_PER_ROUTINE) {
            throw new DataLimitExceedException("할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
        }

        Routine routineToCreate = request.toEntity(userId);
        routineRepository.create(routineToCreate);

        List<RoutineTask> taskListToCreate = routineTaskCreateRequestList.stream()
                .map(taskCreateRequest -> taskCreateRequest.toEntity(routineToCreate.getId()))
                .toList();

        if (!taskListToCreate.isEmpty()) { routineTaskRepository.createBatch(taskListToCreate); }

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
                .routineSummaryInfoList(routineSummaryResponseList)
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse getAllTodayRoutine(Long userId) {
        // 1. (쿼리 1) 사용자 Worker ID 조회
        List<Long> userWorkerIdList = workerRepository.findAllByUserId(userId).stream()
                .map(Worker::getId)
                .toList();

        if (userWorkerIdList.isEmpty()) {
            return RoutineSummaryListResponse.builder().routineSummaryInfoList(Collections.emptyList()).build();
        }

        // 2. (쿼리 2) 오늘의 모든 Work 조회
        List<Work> todayWorkList = workRepository.findAllByWorkerIdListInAndDateRange(userWorkerIdList, LocalDate.now(), LocalDate.now());

        if (todayWorkList.isEmpty()) {
            return RoutineSummaryListResponse.builder().routineSummaryInfoList(Collections.emptyList()).build();
        }

        // 3. (쿼리 3) 오늘 근무에 매핑된 *모든* WorkRoutineMapping을 한 번에 조회
        // 3-1. Work ID 리스트 추출
        List<Long> todayWorkIdList = todayWorkList.stream().map(Work::getId).toList();

        // 3-2. WorkRoutineMappingRepository의 IN 절 쿼리 사용 (위에서 추가한 메서드)
        List<WorkRoutineMapping> allMappings = workRoutineMappingRepository.findAllByWorkIdListIn(todayWorkIdList);

        if (allMappings.isEmpty()) {
            return RoutineSummaryListResponse.builder().routineSummaryInfoList(Collections.emptyList()).build();
        }

        // 4. (쿼리 4) 매핑된 모든 루틴 ID를 한 번에 조회
        // 4-1. Routine ID 리스트 추출 (중복 제거)
        List<Long> allRoutineIds = allMappings.stream()
                .map(WorkRoutineMapping::getRoutineId)
                .distinct()
                .toList();

        // 4-2. 모든 루틴 정보를 한 번에 조회
        List<RoutineSummaryResponse> routineSummaryInfoList = routineRepository.findAllByIdListInAndUserId(allRoutineIds, userId).stream() // 👈 N+1 해결 (2)
                .map(routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime())
                        .build())
                .toList();

        // 5. 결과 반환
        return RoutineSummaryListResponse.builder()
                .routineSummaryInfoList(routineSummaryInfoList)
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
        if (routineTaskUpdateRequestList.size() >= MAX_TASK_COUNT_PER_ROUTINE) {
            throw new DataLimitExceedException("할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
        }

        Routine newRoutine = request.toEntity(routineId, userId);
        routineRepository.update(newRoutine);

        routineTaskRepository.deleteAllByRoutineId(routineId);

        List<RoutineTask> taskListToCreate = routineTaskUpdateRequestList.stream()
                .map(routineTaskUpdateRequest -> routineTaskUpdateRequest.toEntity(routineId))
                .toList();

        if (!taskListToCreate.isEmpty()) { routineTaskRepository.createBatch(taskListToCreate); }
    }

    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        if (routineRepository.existByIdAndUserId(routineId, userId)) {
            workRoutineMappingRepository.deleteByRoutineId(routineId);
            routineTaskRepository.deleteAllByRoutineId(routineId);
            routineRepository.delete(routineId, userId);
        } else {
            throw new RoutineNotFoundException();
        }
    }

    @Transactional
    public void saveWorkRoutineMapping(Long userId, List<Long> routineIdList, Long workId) {
        if (routineIdList.size() >= MAX_ROUTINE_COUNT_PER_WORK) {
            throw new DataLimitExceedException("루틴은 한 근무당 최대 " + MAX_ROUTINE_COUNT_PER_WORK + "개까지 연결할 수 있습니다.");
        }

        // 1. 기존 매핑 모두 삭제 (쿼리 1)
        workRoutineMappingRepository.deleteByWorkId(workId);

        // 1-1. 만약 연결할 루틴이 없다면 여기서 종료
        if (routineIdList.isEmpty()) { return; }

        // 2. 루틴 유효성 검증 (쿼리 2)
        List<Routine> validRoutines = routineRepository.findAllByIdListInAndUserId(routineIdList, userId);

        // 요청한 루틴 ID 개수와 실제 DB에서 찾은 (해당 사용자의) 루틴 개수가 다른 경우
        if (validRoutines.size() != routineIdList.size()) {
            // -> 유효하지 않거나 권한이 없는 ID가 포함된 것이므로 예외 처리
            throw new RoutineNotFoundException("유효하지 않거나 권한이 없는 루틴 ID가 포함되어 있습니다.");
        }

        // 3. 매핑 객체 리스트 생성 (In-Memory 작업)
        List<WorkRoutineMapping> mappingsToCreate = routineIdList.stream()
                .map(routineId -> WorkRoutineMapping.builder()
                        .workId(workId)
                        .routineId(routineId)
                        .build())
                .toList();

        // 4. 배치 삽입 (쿼리 3)
        workRoutineMappingRepository.createBatch(mappingsToCreate);
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> getAllSummarizedRoutineByWorkRoutineMapping(Long userId, Long workId) {
        // 1. 첫 번째 쿼리 (1번 실행)
        List<WorkRoutineMapping> workRoutineMappingList = workRoutineMappingRepository.findAllByWorkId(workId);

        if (workRoutineMappingList.isEmpty()) { return Collections.emptyList(); }

        // 2. 루틴 ID 리스트 추출
        List<Long> routineIdList = workRoutineMappingList.stream()
                .map(WorkRoutineMapping::getRoutineId)
                .toList();

        // 3. 두 번째 쿼리 (1번 실행) - IN 절을 사용해 한 번에 모든 루틴 조회
        List<Routine> routineList = routineRepository.findAllByIdListInAndUserId(routineIdList, userId);

        // 4. (쿼리 없음) 가져온 데이터를 메모리에서 매핑
        return routineList.stream()
                .map(routine -> RoutineSummaryResponse.builder()
                        .routineId(routine.getId())
                        .routineName(routine.getRoutineName())
                        .alarmTime(routine.getAlarmTime())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteWorkRoutineMappingByWorkId(Long workId) {
        workRoutineMappingRepository.deleteByWorkId(workId);
    }
}
