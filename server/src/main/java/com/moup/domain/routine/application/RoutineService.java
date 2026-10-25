package com.moup.domain.routine.application;

import com.moup.domain.routine.dto.RoutineSummaryListResponse;
import com.moup.domain.routine.dto.RoutineSummaryResponse;
import com.moup.domain.routine.domain.RoutineTask;
import com.moup.domain.routine.dto.RoutineTaskCreateRequest;
import com.moup.domain.routine.dto.RoutineTaskDetailResponse;
import com.moup.domain.routine.mapper.RoutineTaskRepository;
import com.moup.domain.routine.dto.RoutineTaskUpdateRequest;
import com.moup.domain.routine.dto.RoutineUpdateRequest;
import com.moup.domain.routine.dto.TodayRoutineResponse;
import com.moup.domain.routine.dto.TodayWorkRoutineCountResponse;
import com.moup.domain.routine.domain.Routine;
import com.moup.domain.routine.dto.RoutineCreateRequest;
import com.moup.domain.routine.dto.RoutineCreateResponse;
import com.moup.domain.routine.dto.RoutineDetailResponse;
import com.moup.domain.routine.exception.RoutineNameAlreadyUsedException;
import com.moup.domain.routine.exception.RoutineNotFoundException;
import com.moup.domain.routine.mapper.RoutineRepository;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.work.domain.WorkRoutineMapping;
import com.moup.domain.work.mapper.WorkRoutineMappingRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.domain.workplace.dto.WorkplaceSummaryResponse;
import com.moup.global.error.DataLimitExceedException;
import com.moup.global.error.InvalidFieldFormatException;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.util.PermissionVerifyUtil;
import com.moup.domain.work.exception.WorkNotFoundException;
import com.moup.domain.user.exception.WorkerNotFoundException;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.work.mapper.WorkRepository.GroupIdAndDayName;
import java.time.DayOfWeek;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineService {

  private static final int MAX_ROUTINE_COUNT_PER_USER = 20; // 사용자당 루틴 연결 최대 개수
  private static final int MAX_TASK_COUNT_PER_ROUTINE = 50; // 루틴당 할 일 연결 최대 개수
  private static final int MAX_ROUTINE_COUNT_PER_WORK = 10; // 근무당 루틴 연결 최대 개수
  private final RoutineRepository routineRepository;
  private final PermissionVerifyUtil permissionVerifyUtil;
  private final RoutineTaskRepository routineTaskRepository;
  private final WorkRoutineMappingRepository workRoutineMappingRepository;
  private final WorkRepository workRepository;
  private final WorkerRepository workerRepository;
  private final WorkplaceRepository workplaceRepository;
  private final com.moup.domain.routine.mapper.RoutineTaskCompletionRepository routineTaskCompletionRepository;

  @Transactional
  public RoutineCreateResponse createRoutine(Long userId, RoutineCreateRequest request) {

    // 1. 루틴 초과 개수 확인
    if (routineRepository.countByUserId(userId) >= MAX_ROUTINE_COUNT_PER_USER) {
      throw new DataLimitExceedException(
          "루틴은 사용자당 최대 " + MAX_ROUTINE_COUNT_PER_USER + "개까지 생성할 수 있습니다.");
    }

    // 2. 중복 루틴 이름 방지?
    if (routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) {
      throw new RoutineNameAlreadyUsedException();
    }

    List<RoutineTaskCreateRequest> routineTaskCreateRequestList = request.getRoutineTaskList();
    if (routineTaskCreateRequestList.size() > MAX_TASK_COUNT_PER_ROUTINE) {
      throw new DataLimitExceedException(
          "할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
    }
    verifyDistinctOrderIndex(routineTaskCreateRequestList.stream().map(RoutineTaskCreateRequest::getOrderIndex).toList());

    // 3. DB Insert Transaction
    // 3-1. 루틴을 먼저 User와 매핑해서 생성
    Routine routineToCreate = request.toEntity(userId);
    routineRepository.create(routineToCreate);

    // 3-2. 생성된 루틴에 루틴 태스크 매핑해서 배치 생성
    List<RoutineTask> taskListToCreate = routineTaskCreateRequestList.stream()
        .map(taskCreateRequest -> taskCreateRequest.toEntity(routineToCreate.getId()))
        .toList();

    if (!taskListToCreate.isEmpty()) {
      routineTaskRepository.createBatch(taskListToCreate);
    }   // 배치로 DB 호출 한번에 생성

    return RoutineCreateResponse.builder()
        .routineId(routineToCreate.getId())
        .build();
  }

  @Transactional(readOnly = true)
  public RoutineSummaryResponse getRoutine(Long userId, Long routineId) {
    Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
        .orElseThrow(RoutineNotFoundException::new);

    List<RoutineSummaryResponse.LinkedWorkRoutine> linkedWorks = getLinkedWorksFromRoutine(routine.getId());

    return RoutineSummaryResponse.builder()
        .routineId(routine.getId())
        .routineName(routine.getRoutineName())
        .alarmTime(routine.getAlarmTime())
        .linkedWorks(linkedWorks)
        .build();
  }

  private List<RoutineSummaryResponse.LinkedWorkRoutine> getLinkedWorksFromRoutine(Long routineId) {
    return getLinkedWorksByRoutineIds(List.of(routineId)).getOrDefault(routineId, List.of());
  }

  /// 루틴별로 연결된 근무와 그 근무의 반복 요일. **루틴 수와 무관하게 쿼리 3번**이다.
  ///
  /// 예전에는 루틴마다 매핑·근무·요일을 따로 조회해, 루틴 20개(상한)면 약 61쿼리였다(스코프 6 I4).
  private Map<Long, List<RoutineSummaryResponse.LinkedWorkRoutine>> getLinkedWorksByRoutineIds(List<Long> routineIds) {
    if (routineIds.isEmpty()) {
      return Map.of();
    }
    List<WorkRoutineMapping> mappings = workRoutineMappingRepository.findAllByRoutineIdIn(routineIds);
    List<Long> workIdList = mappings.stream().map(WorkRoutineMapping::getWorkId).distinct().toList();
    Map<Long, Work> workById = workIdList.isEmpty() ? Map.of()
        : workRepository.findAllByIdIn(workIdList).stream().collect(Collectors.toMap(Work::getId, work -> work));

    // 반복 그룹이 없는 단일 근무는 반복 요일이 없다(빈 배열). NULL을 IN에 넣지 않는다.
    List<String> groupIds = workById.values().stream()
        .map(Work::getRepeatGroupId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<String, List<DayOfWeek>> groupDayMap = new java.util.HashMap<>();
    if (!groupIds.isEmpty()) {
      for (GroupIdAndDayName row : workRepository.findDayNamesByGroupIdsIn(groupIds)) {
        DayOfWeek day = parseDayName(row.dayName());
        if (day != null) {
          groupDayMap.computeIfAbsent(row.groupId(), key -> new ArrayList<>()).add(day);
        }
      }
    }

    Map<Long, List<RoutineSummaryResponse.LinkedWorkRoutine>> result = new java.util.HashMap<>();
    for (WorkRoutineMapping mapping : mappings) {
      Work work = workById.get(mapping.getWorkId());
      if (work == null) {
        continue;
      }
      List<DayOfWeek> repeatDays = work.getRepeatGroupId() == null ? List.of()
          : groupDayMap.getOrDefault(work.getRepeatGroupId(), List.of());
      result.computeIfAbsent(mapping.getRoutineId(), key -> new ArrayList<>())
          .add(new RoutineSummaryResponse.LinkedWorkRoutine(work.getId(), repeatDays));
    }
    return result;
  }

  /// MySQL `DAYNAME()`은 'Monday' 형식이다. 예전 코드는 null 블록에 `continue`가 없어 바로 다음 줄에서
  /// NPE가 났고(스코프 6 M1), 변환 실패는 주석만 있고 로그가 없어 흔적 없이 사라졌다(M2).
  private static DayOfWeek parseDayName(String dayName) {
    if (dayName == null) {
      return null;
    }
    try {
      return DayOfWeek.valueOf(dayName.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("알 수 없는 요일 이름입니다: {}", dayName);
      return null;
    }
  }

  @Transactional(readOnly = true)
  public RoutineSummaryListResponse getAllRoutine(Long userId) {
    List<Routine> routineList = routineRepository.findAllByUserId(userId);
    Map<Long, List<RoutineSummaryResponse.LinkedWorkRoutine>> linkedWorksByRoutine =
        getLinkedWorksByRoutineIds(routineList.stream().map(Routine::getId).toList());

    List<RoutineSummaryResponse> routineSummaryResponseList = routineList.stream()
        .map(routine -> RoutineSummaryResponse.builder()
            .routineId(routine.getId())
            .routineName(routine.getRoutineName())
            .alarmTime(routine.getAlarmTime())
            .linkedWorks(linkedWorksByRoutine.getOrDefault(routine.getId(), List.of()))
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
    List<Work> todayWorkList = workRepository.findAllByWorkerIdListInAndDateRange(userWorkerIdList,
        LocalDate.now(SEOUL_ZONE_ID), LocalDate.now(SEOUL_ZONE_ID));
    if (todayWorkList.isEmpty()) {
      return TodayRoutineResponse.builder()
          .todayWorkRoutineCountList(Collections.emptyList())
          .build();
    }

    List<Long> todayWorkIdList = todayWorkList.stream()
        .map(Work::getId)
        .toList();

    // 4. (쿼리 3) Work ID별 루틴 카운트 Map 조회
    Map<Long, Long> routineCountMap = workRoutineMappingRepository.findCountsByWorkIdListIn(
            todayWorkIdList).stream()
        .collect(Collectors.toMap(WorkRoutineMappingRepository.WorkRoutineCount::workId,
            WorkRoutineMappingRepository.WorkRoutineCount::count));

    // 5. (쿼리 4) Workplace 정보 Map 조회
    // 5-1. Worker Map에서 Workplace ID 리스트 추출
    List<Long> workplaceIdList = workerIdToWorkplaceIdMap.values().stream()
        .distinct()
        .toList();

    Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdListIn(workplaceIdList)
        .stream()
        .collect(Collectors.toMap(Workplace::getId, workplace -> workplace));

    // 6. (In-Memory) DTO 조립
    List<TodayWorkRoutineCountResponse> todayWorkRoutineCountList = todayWorkList.stream()
        .map(work -> {
          // work -> workerId -> workplaceId -> workplace 순서로 조회
          Long workplaceId = workerIdToWorkplaceIdMap.get(work.getWorkerId());
          Workplace workplace = (workplaceId != null) ? workplaceMap.get(workplaceId) : null;

          // 1. 방어 코드 (Workplace가 없으면 null 반환)
            if (workplace == null) {
                return null;
            }

          // 2. 루틴 개수를 먼저 계산하고 0이면 null 반환
          int routineCount = routineCountMap.getOrDefault(work.getId(), 0L).intValue();
          if (routineCount == 0) {
            return null;
          }

          WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
              .workplaceId(workplace.getId())
              .workplaceName(workplace.getWorkplaceName())
              .isShared(workplace.isShared())
              .build();

          return TodayWorkRoutineCountResponse.builder()
              .workId(work.getId())
              .workplaceSummaryInfo(workplaceSummary)
              .startTime(work.getStartTime().atZone(SEOUL_ZONE_ID).toInstant())
              .endTime(
                  work.getEndTime() != null ? work.getEndTime().atZone(SEOUL_ZONE_ID).toInstant()
                      : null)
              // 바로 위에서 null을 가드한 endTime을 그대로 역참조하고 있었다(스코프 6 I6). 진행 중인 근무는 0분이다.
              .workMinutes(work.getEndTime() != null
                  ? Duration.between(work.getStartTime(), work.getEndTime()).toMinutes() : 0L)
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
    List<RoutineSummaryResponse> routineSummaryInfoList = getAllRoutineByWorkRoutineMapping(userId,
        workId);
    return RoutineSummaryListResponse.builder()
        .routineSummaryInfoList(routineSummaryInfoList)
        .build();
  }

  @Transactional(readOnly = true)
  public RoutineDetailResponse getRoutineDetail(Long userId, Long routineId) {
    Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
        .orElseThrow(RoutineNotFoundException::new);
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
        .linkedWorks(getLinkedWorksFromRoutine(routine.getId()))
        .routineTaskList(routineTaskDetailResponseList)
        .build();
  }

  @Transactional
  public void updateRoutine(Long userId, Long routineId, RoutineUpdateRequest request) {
    Routine oldRoutine = routineRepository.findByIdAndUserId(routineId, userId)
        .orElseThrow(RoutineNotFoundException::new);
    if (!oldRoutine.getRoutineName().equals(request.getRoutineName())
        && routineRepository.existByUserIdAndRoutineName(userId, request.getRoutineName())) {
      throw new RoutineNameAlreadyUsedException();
    }

    List<RoutineTaskUpdateRequest> routineTaskUpdateRequestList = request.getRoutineTaskList();
    if (routineTaskUpdateRequestList.size() > MAX_TASK_COUNT_PER_ROUTINE) {
      throw new DataLimitExceedException(
          "할 일은 루틴당 최대 " + MAX_TASK_COUNT_PER_ROUTINE + "개까지 생성할 수 있습니다.");
    }
    verifyDistinctOrderIndex(routineTaskUpdateRequestList.stream().map(RoutineTaskUpdateRequest::getOrderIndex).toList());

    // ID가 온 할 일은 제자리에서 고친다. 예전에는 전부 지우고 다시 만들어, 루틴 이름만 고쳐도 그 할 일들의
    // 오늘 체크가 CASCADE로 전부 풀렸다. ID 없이 보내는 기존 앱은 예전과 똑같이 동작한다(전부 새 할 일).
    Set<Long> existingTaskIds = routineTaskRepository.findAllByRoutineId(routineId).stream()
        .map(RoutineTask::getId)
        .collect(Collectors.toSet());
    List<Long> keptTaskIds = routineTaskUpdateRequestList.stream()
        .map(RoutineTaskUpdateRequest::getTaskId)
        .filter(Objects::nonNull)
        .toList();
    if (!existingTaskIds.containsAll(keptTaskIds) || keptTaskIds.stream().distinct().count() != keptTaskIds.size()) {
      throw new InvalidFieldFormatException("이 루틴의 할 일이 아닌 ID가 있거나 같은 ID가 두 번 있습니다.");
    }

    Routine newRoutine = request.toEntity(routineId, userId);
    routineRepository.update(newRoutine);

    routineTaskRepository.deleteAllByRoutineIdExcept(routineId, keptTaskIds);
    routineTaskRepository.moveOrderIndexesOutOfTheWay(routineId);
    for (RoutineTaskUpdateRequest task : routineTaskUpdateRequestList) {
      if (task.getTaskId() != null) {
        routineTaskRepository.updateContentAndOrder(task.getTaskId(), routineId, task.getContent(), task.getOrderIndex());
      }
    }

    List<RoutineTask> taskListToCreate = routineTaskUpdateRequestList.stream()
        .filter(task -> task.getTaskId() == null)
        .map(routineTaskUpdateRequest -> routineTaskUpdateRequest.toEntity(routineId))
        .toList();

    if (!taskListToCreate.isEmpty()) {
      routineTaskRepository.createBatch(taskListToCreate);
    }
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
    if (routineIdList.size() > MAX_ROUTINE_COUNT_PER_WORK) {
      throw new DataLimitExceedException(
          "루틴은 한 근무당 최대 " + MAX_ROUTINE_COUNT_PER_WORK + "개까지 연결할 수 있습니다.");
    }

    // 1. 기존 매핑 모두 삭제 (쿼리 1)
    workRoutineMappingRepository.deleteByWorkId(workId);

    // 1-1. 만약 연결할 루틴이 없다면 여기서 종료
    if (routineIdList.isEmpty()) {
      return;
    }

    // 2. 루틴 유효성 검증 (쿼리 2)
    List<Routine> validRoutines = routineRepository.findAllByIdListInAndUserId(routineIdList,
        userId);

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

  /// 방금 만든 근무들에 같은 루틴을 한 번에 연결한다. 검증 1번 + INSERT 1번.
  ///
  /// 예전에는 반복 근무마다 `saveWorkRoutineMapping`을 불러 근무당 DELETE·SELECT·INSERT 3개,
  /// 365일 반복이면 한 트랜잭션에서 약 780~1,100쿼리였다(스코프 2 I9). 새 근무라 지울 매핑도 없다.
  @Transactional
  public void linkRoutinesToNewWorks(Long userId, List<Long> routineIdList, List<Long> workIds) {
    if (routineIdList.isEmpty() || workIds.isEmpty()) {
      return;
    }
    if (routineIdList.size() > MAX_ROUTINE_COUNT_PER_WORK) {
      throw new DataLimitExceedException(
          "루틴은 한 근무당 최대 " + MAX_ROUTINE_COUNT_PER_WORK + "개까지 연결할 수 있습니다.");
    }
    if (routineRepository.findAllByIdListInAndUserId(routineIdList, userId).size() != routineIdList.size()) {
      throw new RoutineNotFoundException("유효하지 않거나 권한이 없는 루틴 ID가 포함되어 있습니다.");
    }
    List<WorkRoutineMapping> mappings = new ArrayList<>(workIds.size() * routineIdList.size());
    for (Long workId : workIds) {
      for (Long routineId : routineIdList) {
        mappings.add(WorkRoutineMapping.builder().workId(workId).routineId(routineId).build());
      }
    }
    workRoutineMappingRepository.createBatch(mappings);
  }

  @Transactional(readOnly = true)
  public List<RoutineSummaryResponse> getAllRoutineByWorkRoutineMapping(Long userId, Long workId) {
    // --- START: 권한 확인 ---
    Work work = workRepository.findById(workId)
        .orElseThrow(WorkNotFoundException::new);

    Worker worker = workerRepository.findById(work.getWorkerId())
        .orElseThrow(WorkerNotFoundException::new);

    Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId())
        .orElseThrow(WorkplaceNotFoundException::new);

    // 확정 정책 15 — 미승인 근무자의 근무에는 루틴을 연결할 수 없고 조회도 막는다.
    // 사장님은 통과시킨다(자기 근무지 알바생의 근무를 다룰 수 있다).
    permissionVerifyUtil.verifyWorkerPermission(userId, worker, workplace.getOwnerId());
    // --- END: 권한 확인 ---

    // 1. 첫 번째 쿼리 (1번 실행)
    List<WorkRoutineMapping> workRoutineMappingList = workRoutineMappingRepository.findAllByWorkId(
        workId);

    if (workRoutineMappingList.isEmpty()) {
      return Collections.emptyList();
    }

    // 2. 루틴 ID 리스트 추출
    List<Long> routineIdList = workRoutineMappingList.stream()
        .map(WorkRoutineMapping::getRoutineId)
        .distinct()
        .toList();

    if (routineIdList.isEmpty()) {
      return Collections.emptyList();
    }

    // 확정 정책 14 — 사장님은 알바생 근무에 연결된 루틴의 **제목만** 본다. 예전에는 루틴을 요청자(사장님)
    // 소유로만 찾아 항상 빈 배열이었다(스코프 6 I1). 루틴은 그 근무의 알바생 것이므로 알바생 소유로 찾는다.
    boolean isOwnerView = Objects.equals(workplace.getOwnerId(), userId) && !Objects.equals(worker.getUserId(), userId);
    if (isOwnerView) {
      if (worker.getUserId() == null) {
        return Collections.emptyList();
      }
      // 할 일·알람 시각·연결된 다른 근무는 뺀다. linkedWorks에는 알바생의 **다른 근무지** 근무까지 들어 있다.
      return routineRepository.findAllByIdListInAndUserId(routineIdList, worker.getUserId()).stream()
          .map(routine -> RoutineSummaryResponse.builder()
              .routineId(routine.getId())
              .routineName(routine.getRoutineName())
              .build())
          .toList();
    }

    // 3. 두 번째 쿼리 (1번 실행) - IN 절을 사용해 한 번에 모든 루틴 조회
    List<Routine> routineList = routineRepository.findAllByIdListInAndUserId(routineIdList, userId);

    // 4. [쿼리 1회] 이 근무에서 완료된 할 일. 근무 하나당 한 번이면 충분하다.
    Set<Long> completedTaskIds =
        Set.copyOf(routineTaskCompletionRepository.findCompletedTaskIdsByWorkId(workId));
    // 루틴마다 할 일·연결 근무를 따로 읽지 않는다(스코프 6 I4).
    List<Long> loadedRoutineIds = routineList.stream().map(Routine::getId).toList();
    Map<Long, List<RoutineSummaryResponse.LinkedWorkRoutine>> linkedWorksByRoutine = getLinkedWorksByRoutineIds(loadedRoutineIds);
    Map<Long, List<RoutineTask>> tasksByRoutine = loadedRoutineIds.isEmpty() ? Map.of()
        : routineTaskRepository.findAllByRoutineIdIn(loadedRoutineIds).stream()
            .collect(Collectors.groupingBy(RoutineTask::getRoutineId));

    // 5. 가져온 데이터를 매핑. 할 일과 완료 여부를 함께 내려준다 —
    //    루틴 상세 조회는 근무를 모르므로 체크 상태를 줄 수 없고,
    //    이 화면이 체크박스를 그릴 수 있는 유일한 지점이다.
    return routineList.stream()
        .map(routine -> RoutineSummaryResponse.builder()
            .routineId(routine.getId())
            .routineName(routine.getRoutineName())
            .alarmTime(routine.getAlarmTime())
            .linkedWorks(linkedWorksByRoutine.getOrDefault(routine.getId(), List.of()))
            .routineTaskList(tasksByRoutine.getOrDefault(routine.getId(), List.of()).stream()
                .map(task -> RoutineTaskDetailResponse.builder()
                    .taskId(task.getId())
                    .routineId(task.getRoutineId())
                    .content(task.getContent())
                    .orderIndex(task.getOrderIndex())
                    .isCompleted(completedTaskIds.contains(task.getId()))
                    .build())
                .toList())
            .build())
        .toList();
  }

  /// 근무별 할 일 체크/해제.
  ///
  /// 완료는 (근무, 할 일) 쌍이다. 같은 루틴이 여러 근무에 연결되므로 할 일 하나에
  /// 플래그를 두면 어제 근무의 체크가 오늘 근무에 그대로 비친다.
  ///
  /// 저장소의 UNIQUE와 `ON DUPLICATE KEY UPDATE` 덕에 멱등하다 — 더블탭이나
  /// 네트워크 재시도로 같은 요청이 두 번 와도 상태가 어긋나지 않는다.
  @Transactional
  public void setRoutineTaskCompletion(Long userId, Long workId, Long taskId, boolean completed) {
    Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
    Worker worker = workerRepository.findById(work.getWorkerId())
        .orElseThrow(WorkerNotFoundException::new);
    Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId())
        .orElseThrow(WorkplaceNotFoundException::new);

    permissionVerifyUtil.verifyWorkerPermission(userId, worker, workplace.getOwnerId());
    // 체크도 근무 기록이다. 사장님이 탈퇴했거나 근무가 끝난 근무지에서는 막는다 (확정 정책 5, D11-5).
    permissionVerifyUtil.verifyWritable(worker, workplace.getOwnerId());

    // 이 근무에 연결되지 않은 루틴의 할 일은 체크할 수 없다.
    // FK는 "그 할 일이 존재하는가"만 보장할 뿐, 남의 루틴 할 일 id를 넣는 것을 막지 못한다.
    Set<Long> linkedRoutineIds = workRoutineMappingRepository.findAllByWorkId(workId).stream()
        .map(WorkRoutineMapping::getRoutineId)
        .collect(Collectors.toSet());
    RoutineTask task = routineTaskRepository.findById(taskId)
        .orElseThrow(RoutineNotFoundException::new);
    if (!linkedRoutineIds.contains(task.getRoutineId())) {
      throw new InvalidPermissionAccessException("이 근무에 연결되지 않은 루틴의 할 일입니다.");
    }

    if (completed) {
      routineTaskCompletionRepository.complete(workId, taskId);
    } else {
      routineTaskCompletionRepository.uncomplete(workId, taskId);
    }
  }

  @Transactional
  public void deleteWorkRoutineMappingByWorkId(Long workId) {
    workRoutineMappingRepository.deleteByWorkId(workId);
  }

  /// 같은 루틴 안에서 정렬 순서가 겹치면 `UNIQUE (routine_id, order_index)`에 걸려 422가 아니라 500이 났다(스코프 6 M6).
  private static void verifyDistinctOrderIndex(List<Integer> orderIndexes) {
    if (orderIndexes.stream().distinct().count() != orderIndexes.size()) {
      throw new InvalidFieldFormatException("할 일의 정렬 순서(orderIndex)가 겹칩니다.");
    }
  }
}
