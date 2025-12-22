package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.moup.server.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final RoutineTaskRepository routineTaskRepository;
    private final WorkRoutineMappingRepository workRoutineMappingRepository;
    private final WorkRepository workRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;

    private static final int MAX_ROUTINE_COUNT_PER_USER = 20; // 사용자당 루틴 연결 최대 개수
    private static final int MAX_TASK_COUNT_PER_ROUTINE = 50; // 루틴당 할 일 연결 최대 개수
    private static final int MAX_ROUTINE_COUNT_PER_WORK = 10; // 근무당 루틴 연결 최대 개수

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
    public RoutineSummaryResponse getRoutine(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);

        return RoutineSummaryResponse.builder()
                .routineId(routine.getId())
                .routineName(routine.getRoutineName())
                .alarmTime(routine.getAlarmTime())
                .build();
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse getAllRoutine(Long userId) {
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
    public TodayRoutineResponse getAllTodayWorkplaceRoutineCount(Long userId) {
        // 1. (쿼리 1) 사용자의 모든 Worker 정보 조회
        List<Worker> userWorkerList = workerRepository.findAllByUserId(userId);
        if (userWorkerList.isEmpty()) {
            return TodayRoutineResponse.builder()
                    .todayWorkRoutineCountList(Collections.emptyList())
                    .build();
        }

        // 2. (In-Memory) Worker ID 리스트 및 WorkerId -> WorkplaceId Map 생성 (N+1 방지용)
        List<Long> userWorkerIdList = userWorkerList.stream()
                .map(Worker::getId)
                .toList();

        Map<Long, Long> workerIdToWorkplaceIdMap = userWorkerList.stream()
                .collect(Collectors.toMap(Worker::getId, Worker::getWorkplaceId));

        // 3. (쿼리 2) 오늘의 모든 Work 조회
        List<Work> todayWorkList = workRepository.findAllByWorkerIdListInAndDateRange(userWorkerIdList, LocalDate.now(SEOUL_ZONE_ID), LocalDate.now(SEOUL_ZONE_ID));
        if (todayWorkList.isEmpty()) {
            return TodayRoutineResponse.builder()
                    .todayWorkRoutineCountList(Collections.emptyList())
                    .build();
        }

        List<Long> todayWorkIdList = todayWorkList.stream()
                .map(Work::getId)
                .toList();

        // 4. (쿼리 3) Work ID별 루틴 카운트 Map 조회
        Map<Long, Long> routineCountMap = workRoutineMappingRepository.findCountsByWorkIdListIn(todayWorkIdList).stream()
                .collect(Collectors.toMap(WorkRoutineMappingRepository.WorkRoutineCount::workId,
                        WorkRoutineMappingRepository.WorkRoutineCount::count));

        // 5. (쿼리 4) Workplace 정보 Map 조회
        // 5-1. Worker Map에서 Workplace ID 리스트 추출
        List<Long> workplaceIdList = workerIdToWorkplaceIdMap.values().stream()
                .distinct()
                .toList();

        Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdListIn(workplaceIdList).stream()
                .collect(Collectors.toMap(Workplace::getId, workplace -> workplace));

        // 6. (In-Memory) DTO 조립
        List<TodayWorkRoutineCountResponse> todayWorkRoutineCountList = todayWorkList.stream()
                .map(work -> {
                    // work -> workerId -> workplaceId -> workplace 순서로 조회
                    Long workplaceId = workerIdToWorkplaceIdMap.get(work.getWorkerId());
                    Workplace workplace = (workplaceId != null) ? workplaceMap.get(workplaceId) : null;

                    // 1. 방어 코드 (Workplace가 없으면 null 반환)
                    if (workplace == null) return null;

                    // 2. 루틴 개수를 먼저 계산하고 0이면 null 반환
                    int routineCount = routineCountMap.getOrDefault(work.getId(), 0L).intValue();
                    if (routineCount == 0) { return null; }

                    WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                            .workplaceId(workplace.getId())
                            .workplaceName(workplace.getWorkplaceName())
                            .isShared(workplace.isShared())
                            .build();

                    return TodayWorkRoutineCountResponse.builder()
                            .workId(work.getId())
                            .workplaceSummaryInfo(workplaceSummary)
                            .startTime(work.getStartTime().atZone(SEOUL_ZONE_ID).toInstant())
                            .endTime(work.getEndTime() != null ? work.getEndTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                            .workMinutes(Duration.between(work.getStartTime(), work.getEndTime()).toMinutes())
                            .routineCount(routineCount)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        // 7. 결과 반환
        return TodayRoutineResponse.builder()
                .todayWorkRoutineCountList(todayWorkRoutineCountList)
                .build();
    }

    @Transactional(readOnly = true)
    public int getTodayTotalRoutineCount(Long userId, LocalDate date) {
        Integer count = routineRepository.countTotalRoutinesByUserIdAndDate(userId, date);

        return (count != null) ? count : 0;
    }

    @Transactional(readOnly = true)
    public RoutineSummaryListResponse getAllRoutineByWork(Long userId, Long workId) {
        List<RoutineSummaryResponse> routineSummaryInfoList = getAllRoutineByWorkRoutineMapping(userId, workId);

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
    public List<RoutineSummaryResponse> getAllRoutineByWorkRoutineMapping(Long userId, Long workId) {
        // --- START: 권한 확인 ---
        Work work = workRepository.findById(workId)
                .orElseThrow(WorkNotFoundException::new);

        Worker worker = workerRepository.findByIdAndUserId(work.getWorkerId(), userId)
                .orElseThrow(WorkerNotFoundException::new);

        Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId())
                .orElseThrow(WorkplaceNotFoundException::new);

        if (!userId.equals(worker.getUserId()) && !userId.equals(workplace.getOwnerId())) {
            throw new InvalidPermissionAccessException();
        }
        // --- END: 권한 확인 ---

        // 1. 첫 번째 쿼리 (1번 실행)
        List<WorkRoutineMapping> workRoutineMappingList = workRoutineMappingRepository.findAllByWorkId(workId);

        if (workRoutineMappingList.isEmpty()) { return Collections.emptyList(); }

        // 2. 루틴 ID 리스트 추출
        List<Long> routineIdList = workRoutineMappingList.stream()
                .map(WorkRoutineMapping::getRoutineId)
                .distinct()
                .toList();

        if (routineIdList.isEmpty()) { return Collections.emptyList(); }

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
