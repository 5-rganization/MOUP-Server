package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.repository.*;
import com.moup.server.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/// 근무(Work) 관련 비즈니스 로직을 처리하는 서비스 클래스
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {

    // --- 의존성 주입 ---
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;
    private final UserRepository userRepository;
    private final RoutineService routineService;
    private final SalaryCalculationService salaryCalculationService;
    private final PermissionVerifyUtil permissionVerifyUtil;

    // --- 상수 ---
    private static final long MAX_REPEAT_DAYS_LIMIT = 365L; // 반복 생성 최대 기간

    // --- 내부 레코드 (데이터 전달용) ---
    /// 근무 조회 시 권한 검증 후 필요한 데이터를 담는 레코드
    private record VerifiedWorkContextForRead(
            Work work, long workMinutes, Worker worker,
            WorkerSummaryResponse workerSummaryInfo, WorkplaceSummaryResponse workplaceSummaryInfo, boolean isEditable
    ) {}

    /// 근무 수정/삭제 시 권한 검증 후 필요한 데이터를 담는 레코드
    private record VerifiedWorkContextForUD(Work work, Worker worker) {}

    /// 반복 정보 캐싱용 레코드
    private record RepeatInfo(List<DayOfWeek> days, LocalDate endDate) {}

    /// 캘린더 조회 시 미리 로드된 근무 데이터 및 반복 정보 캐시를 담는 레코드
    private record CalendarWorkData(
            List<Work> allWorks, // 반복 정보 캐싱에 사용될 전체 근무 목록
            Map<Long, List<Work>> workMapByWorker, // Worker ID별로 그룹화된 근무 목록
            Map<String, RepeatInfo> repeatInfoCache // 미리 생성된 반복 정보 캐시
    ) {}

    /// 근무 업데이트 결과용 레코드
    public record UpdateWorkResult(boolean recurringCreatedOrReplaced, List<Long> resultingWorkIds) {}

    // =================================================================
    // 근무 생성 (Create)
    // =================================================================

    /// 사용자가 자신의 근무를 생성합니다 (단일 또는 반복).
    /// 반복 근무 생성 시 모든 인스턴스에 루틴을 연결합니다.
    /// @param userId 요청 사용자 ID
    /// @param workplaceId 근무지 ID
    /// @param request 근무 생성 요청 DTO
    /// @return 생성된 모든 근무 ID 배열 응답 DTO
    @Transactional
    public WorkCreateResponse createMyWork(Long userId, Long workplaceId, MyWorkCreateRequest request) {
        // 근무자 정보 및 권한 확인
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        // 근무 생성 (단일 또는 반복), 생성된 모든 Work 객체 리스트 반환
        List<Work> createdWorks = createMyWorkHelper(userWorker, request);

        // 모든 생성된 근무에 루틴 연결
        if (request.getRoutineIdList() != null && !request.getRoutineIdList().isEmpty()) {
            for (Work work : createdWorks) {
                routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());
            }
        }

        // 생성된 모든 근무 ID 추출
        List<Long> createdWorkIds = createdWorks.stream()
                .map(Work::getId)
                .filter(Objects::nonNull) // ID가 null인 경우 방지 (MyBatis 배치 설정 따라)
                .collect(Collectors.toList());

        // 모든 ID 리스트를 DTO에 담아 반환
        return WorkCreateResponse.builder()
                .workId(createdWorkIds)
                .build();
    }

    /// 사장님이 특정 근무자의 근무를 생성합니다 (단일 또는 반복).
    /// 루틴은 연결하지 않습니다.
    /// @param requesterUserId 요청 사장님 ID
    /// @param workplaceId 근무지 ID
    /// @param workerId 대상 근무자(Worker) ID
    /// @param request 근무 생성 요청 DTO
    /// @return 생성된 모든 근무 ID 배열 응답 DTO
    @Transactional
    public WorkCreateResponse createWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, WorkerWorkCreateRequest request) {
        // 근무자 정보 및 사장님 권한 확인
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new);
        if (worker.getUserId() == null) { throw new WorkerNotFoundException("이미 탈퇴한 근무자입니다."); }
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(requesterUserId, workplaceOwnerId);

        // 근무 생성 (단일 또는 반복), 생성된 모든 Work 객체 리스트 반환
        List<Work> createdWorks = createWorkForWorkerHelper(worker, request);

        // 사장님이 생성 시 루틴은 연결하지 않음

        // 생성된 모든 근무 ID 추출
        List<Long> createdWorkIds = createdWorks.stream()
                .map(Work::getId)
                .filter(Objects::nonNull) // ID가 null인 경우 방지
                .collect(Collectors.toList());

        // 모든 ID 리스트를 DTO에 담아 반환
        return WorkCreateResponse.builder()
                .workId(createdWorkIds)
                .build();
    }

    // =================================================================
    // 근무 조회 (Read)
    // =================================================================

    /// 특정 근무의 상세 정보를 조회합니다.
    /// @param userId 요청 사용자 ID
    /// @param workId 조회할 근무 ID
    /// @return 근무 상세 정보 DTO
    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workId) {
        VerifiedWorkContextForRead context = getVerifiedWorkContextForRead(userId, workId);
        List<RoutineSummaryResponse> routineSummaryList = routineService.getAllRoutineByWorkRoutineMapping(userId, workId);

        // 반복 정보 조회 및 변환
        List<DayOfWeek> repeatDays = Collections.emptyList();
        LocalDate repeatEndDate = null;
        if (context.work().getRepeatGroupId() != null) {
            RepeatInfo repeatInfo = getRepeatInfo(context.work().getRepeatGroupId());
            repeatDays = repeatInfo.days();
            repeatEndDate = repeatInfo.endDate();
        }

        return WorkDetailResponse.builder()
                .workId(context.work().getId())
                .workerSummaryInfo(context.workerSummaryInfo())
                .workplaceSummaryInfo(context.workplaceSummaryInfo())
                .routineSummaryInfoList(routineSummaryList)
                .workDate(context.work().getWorkDate())
                .startTime(context.work().getStartTime())
                .actualStartTime(context.work().getActualStartTime())
                .endTime(context.work().getEndTime())
                .actualEndTime(context.work().getActualEndTime())
                .restTimeMinutes(context.work().getRestTimeMinutes())
                .workMinutes(context.workMinutes())
                .memo(context.work().getMemo())
                .repeatDays(repeatDays)
                .repeatEndDate(repeatEndDate)
                .isMyWork(checkIsMyWork(userId, context.worker().getUserId()))
                .isEditable(context.isEditable())
                .build();
    }

    /// 특정 근무의 요약 정보를 조회합니다.
    /// @param userId 요청 사용자 ID
    /// @param workId 조회할 근무 ID
    /// @return 근무 요약 정보 DTO
    @Transactional(readOnly = true)
    public WorkSummaryResponse getWork(Long userId, Long workId) {
        VerifiedWorkContextForRead context = getVerifiedWorkContextForRead(userId, workId);

        // 반복 정보 조회 및 변환
        List<DayOfWeek> repeatDays = Collections.emptyList();
        LocalDate repeatEndDate = null;
        if (context.work().getRepeatGroupId() != null) {
            RepeatInfo repeatInfo = getRepeatInfo(context.work().getRepeatGroupId());
            repeatDays = repeatInfo.days();
            repeatEndDate = repeatInfo.endDate();
        }

        return WorkSummaryResponse.builder()
                .workId(context.work().getId())
                .workerSummaryInfo(context.workerSummaryInfo())
                .workplaceSummaryInfo(context.workplaceSummaryInfo())
                .workDate(context.work().getWorkDate())
                .startTime(context.work().getStartTime())
                .endTime(context.work().getEndTime())
                .workMinutes(context.workMinutes())
                .restTimeMinutes(context.work().getRestTimeMinutes())
                .estimatedNetIncome(context.work().getEstimatedNetIncome())
                .repeatDays(repeatDays)
                .repeatEndDate(repeatEndDate)
                .isMyWork(checkIsMyWork(userId, context.worker().getUserId()))
                .isEditable(context.isEditable())
                .build();
    }

    /// 사용자의 모든 근무 기록을 특정 기간 기준으로 조회합니다 (캘린더용).
    /// @param userId 요청 사용자 ID
    /// @param baseYearMonth 기준 년월 (이 월의 -6개월 ~ +6개월 범위 조회)
    /// @return 근무 요약 정보 리스트 DTO
    @Transactional(readOnly = true)
    public WorkCalendarListResponse getAllMyWork(Long userId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // --- 사용자 및 근무지 관련 정보 로드 ---
        List<Worker> userWorkerList = workerRepository.findAllByUserId(userId);
        if (userWorkerList.isEmpty()) { return WorkCalendarListResponse.builder().workSummaryInfoList(Collections.emptyList()).build(); }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        List<Long> workplaceIdList = userWorkerList.stream().map(Worker::getWorkplaceId).distinct().toList();
        List<Long> workerIdList = userWorkerList.stream().map(Worker::getId).toList();
        Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdListIn(workplaceIdList).stream().collect(Collectors.toMap(Workplace::getId, w -> w));

        // --- 근무 데이터 및 캐시 로드 ---
        CalendarWorkData calendarData = preloadCalendarWorkData(workerIdList, startDate, endDate);
        Map<Long, List<Work>> workMapByWorker = calendarData.workMapByWorker();
        Map<String, RepeatInfo> repeatInfoCache = calendarData.repeatInfoCache();
        // --------------------------------------------------------

        // DTO 조립 (반복 정보 캐시 사용)
        List<WorkSummaryResponse> userWorkSummaryList = new ArrayList<>();
        for (Worker userWorker : userWorkerList) {
            Workplace workplace = workplaceMap.get(userWorker.getWorkplaceId());
            if (workplace == null) continue;
            WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(userWorker, user);
            WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId()).workplaceName(workplace.getWorkplaceName()).isShared(workplace.isShared()).build();
            List<Work> workerWorkList = workMapByWorker.getOrDefault(userWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workSummaryList = workerWorkList.stream().map(work -> {
                long workMinutes = work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0;
                return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummaryInfo, workMinutes, true, true, repeatInfoCache);
            }).toList();
            userWorkSummaryList.addAll(workSummaryList);
        }
        return WorkCalendarListResponse.builder().workSummaryInfoList(userWorkSummaryList).build();
    }

    /// 특정 근무지의 모든 근무 기록을 특정 기간 기준으로 조회합니다 (캘린더용).
    /// @param user 요청 사용자 (사장님 또는 해당 근무지 근무자)
    /// @param workplaceId 조회할 근무지 ID
    /// @param baseYearMonth 기준 년월
    /// @return 근무 요약 정보 리스트 DTO
    @Transactional(readOnly = true)
    public WorkCalendarListResponse getAllWorkByWorkplace(User user, Long workplaceId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // --- 근무지 및 근무자 관련 정보 로드 ---
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        Optional<Worker> requesterWorkerOpt = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId);
        if (requesterWorkerOpt.isEmpty() && !workplace.getOwnerId().equals(user.getId())) { throw new InvalidPermissionAccessException(); }
        WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId()).workplaceName(workplace.getWorkplaceName()).isShared(workplace.isShared()).build();
        List<Worker> workplaceWorkerList = workerRepository.findAllByWorkplaceId(workplaceId);
        if (workplaceWorkerList.isEmpty()) { return WorkCalendarListResponse.builder().workSummaryInfoList(Collections.emptyList()).build(); }
        List<Long> workerIdList = workplaceWorkerList.stream().map(Worker::getId).toList(); // 헬퍼 메서드 호출에 필요
        List<Long> userIdList = workplaceWorkerList.stream().map(Worker::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllByIdListIn(userIdList).stream().collect(Collectors.toMap(User::getId, u -> u));

        // --- 근무 데이터 및 캐시 로드 ---
        CalendarWorkData calendarData = preloadCalendarWorkData(workerIdList, startDate, endDate);
        Map<Long, List<Work>> workMapByWorker = calendarData.workMapByWorker();
        Map<String, RepeatInfo> repeatInfoCache = calendarData.repeatInfoCache();
        // --------------------------------------------------------

        // DTO 조립 (반복 정보 캐시 사용)
        List<WorkSummaryResponse> workSummaryInfoList = new ArrayList<>();
        for (Worker workplaceWorker : workplaceWorkerList) {
            User workerUser = null;
            if (workplaceWorker.getUserId() != null) { workerUser = userMap.get(workplaceWorker.getUserId()); }
            WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(workplaceWorker, workerUser); // User 객체 전달
            List<Work> workerWorkList = workMapByWorker.getOrDefault(workplaceWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workerWorkSummaryList = workerWorkList.stream().map(work -> {
                long workMinutes = work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0;
                boolean isMyWork = checkIsMyWork(user.getId(), workplaceWorker.getUserId());
                boolean isEditable = checkEditable(user.getId(), workplaceWorker.getUserId(), workplace.getOwnerId());
                // workplaceSummaryInfo는 루프 밖에서 미리 생성한 것 사용
                return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummaryInfo, workMinutes, isMyWork, isEditable, repeatInfoCache);
            }).toList();
            workSummaryInfoList.addAll(workerWorkSummaryList);
        }
        return WorkCalendarListResponse.builder().workSummaryInfoList(workSummaryInfoList).build();
    }

    /// 특정 근무지에서 사용자의 근무 기록만 특정 기간 기준으로 조회합니다 (캘린더용).
    /// @param user 요청 사용자
    /// @param workplaceId 조회할 근무지 ID
    /// @param baseYearMonth 기준 년월
    /// @return 근무 요약 정보 리스트 DTO
    public WorkCalendarListResponse getAllMyWorkByWorkplace(User user, Long workplaceId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // 관련 정보 로드
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId()).workplaceName(workplace.getWorkplaceName()).isShared(workplace.isShared()).build();
        WorkerSummaryResponse userWorkerSummaryInfo = createWorkerSummary(userWorker, user);
        List<Work> userWorkList = workRepository.findAllByWorkerIdAndDateRange(userWorker.getId(), startDate, endDate);

        // 반복 정보 캐시 생성
        Map<String, RepeatInfo> repeatInfoCache = prefetchRepeatInfo(userWorkList);

        // DTO 변환
        List<WorkSummaryResponse> workSummaryInfoList = userWorkList.stream().map(userWork -> {
            long workMinutes = userWork.getNetWorkMinutes() != null ? userWork.getNetWorkMinutes() : 0;
            // convertWorkToSummaryResponse 호출 시 캐시 전달
            return convertWorkToSummaryResponse(userWork, userWorkerSummaryInfo, workplaceSummary, workMinutes, true, true, repeatInfoCache);
        }).toList();
        return WorkCalendarListResponse.builder().workSummaryInfoList(workSummaryInfoList).build();
    }

    // =================================================================
    // 근무 수정 (Update)
    // =================================================================

    /// 사용자가 자신의 근무 기록을 수정합니다.
    /// - DTO의 반복 설정에 따라 단일 또는 반복 업데이트를 수행합니다.
    /// @param requesterUserId 요청 사용자 ID
    /// @param workId 수정할 근무 ID
    /// @param request 근무 수정 요청 DTO
    /// @return UpdateWorkResult (반복 생성 여부 및 결과 ID 리스트 포함)
    @Transactional
    public UpdateWorkResult updateMyWork(Long requesterUserId, Long workId, MyWorkUpdateRequest request) {
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);
        if (!Objects.equals(context.worker().getUserId(), requesterUserId)) { throw new InvalidPermissionAccessException("본인의 근무 기록만 수정할 수 있습니다."); }

        List<Work> resultingWorks; // 결과를 담을 리스트
        boolean recurringReplaced = false; // 반복 대체 여부 플래그

        if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
            // --- 반복 중단 또는 단일 근무 수정 ---
            stopRecurrenceAndUpdateSingle(context.worker(), context.work(), request.getStartTime(), request.getEndTime(),
                    request.getActualStartTime(), request.getActualEndTime(), request.getRestTimeMinutes(), request.getMemo());
            // 단일 업데이트 후에는 해당 workId 하나만 결과로 간주
            resultingWorks = List.of(context.work().toBuilder().id(workId).build()); // ID만 있는 임시 객체
        } else {
            // --- 새로운 반복 시작 또는 기존 반복 변경 ---
            resultingWorks = replaceWithNewRecurringWorks(context.worker(), context.work(), request.getStartTime(), request.getEndTime(),
                    request.getRestTimeMinutes(), request.getMemo(), request.getRepeatDays(), request.getRepeatEndDate());
            recurringReplaced = true;
        }

        // 루틴 연결 (첫 근무 또는 단일 근무에)
        if (!resultingWorks.isEmpty()) {
            routineService.saveWorkRoutineMapping(context.worker().getUserId(), request.getRoutineIdList(), resultingWorks.get(0).getId());
        }

        // 결과 ID 리스트 추출
        List<Long> resultingWorkIds = resultingWorks.stream()
                .map(Work::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new UpdateWorkResult(recurringReplaced, resultingWorkIds);
    }

    /// 사장님이 특정 근무자의 근무 기록을 수정합니다.
    /// - DTO의 반복 설정에 따라 단일 또는 반복 업데이트를 수행합니다.
    /// @param requesterUserId 요청 사장님 ID
    /// @param workplaceId 근무지 ID
    /// @param workerId 대상 근무자(Worker) ID
    /// @param workId 수정할 근무 ID
    /// @param request 근무 수정 요청 DTO
    /// @return UpdateWorkResult (반복 생성 여부 및 결과 ID 리스트 포함)
    @Transactional
    public UpdateWorkResult updateWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, Long workId, WorkerWorkUpdateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(requesterUserId, workplace.getOwnerId());
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
        if (!work.getWorkerId().equals(worker.getId())) { throw new BadRequestException("해당 근무 기록은 지정된 근무자의 것이 아닙니다."); }

        List<Work> resultingWorks;
        boolean recurringReplaced = false;

        if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
            // --- 반복 중단 또는 단일 근무 수정 ---
            stopRecurrenceAndUpdateSingle(worker, work, request.getStartTime(), request.getEndTime(),
                    request.getActualStartTime(), request.getActualEndTime(), request.getRestTimeMinutes(), request.getMemo());
            resultingWorks = List.of(work.toBuilder().id(workId).build());
            recurringReplaced = false;
        } else {
            // --- 새로운 반복 시작 또는 기존 반복 변경 ---
            resultingWorks = replaceWithNewRecurringWorks(worker, work, request.getStartTime(), request.getEndTime(),
                    request.getRestTimeMinutes(), request.getMemo(), request.getRepeatDays(), request.getRepeatEndDate());
            recurringReplaced = true;
        }

        // 사장님이 수정 시 루틴 매핑은 건드리지 않음

        List<Long> resultingWorkIds = resultingWorks.stream()
                .map(Work::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new UpdateWorkResult(recurringReplaced, resultingWorkIds);
    }

    /// 사용자의 실제 출근 시간을 기록합니다.
    /// @param userId 요청 사용자 ID
    /// @param workplaceId 근무지 ID
    /// @return 출근 기록 대상 근무가 존재하면 true, 없으면 false
    @Transactional
    public boolean updateActualStartTime(Long userId, Long workplaceId) {
        // 권한 및 근무 중복 확인
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);
        if (workerRepository.existsByUserIdAndIsNowWorking(userId, true)) { throw new WorkerAlreadyWorkingException(); }

        // 현재 시간 기준으로 출근 가능한 근무 조회
        LocalDateTime currentDateTime = LocalDateTime.now();
        Optional<Work> optWorkToStart = workRepository.findEligibleWorkForClockIn(userWorker.getId(), currentDateTime);

        // 출근 기록 업데이트 및 상태 변경
        if (optWorkToStart.isPresent()) {
            Work workToStart = optWorkToStart.get();
            workRepository.updateActualStartTimeById(workToStart.getId(), currentDateTime);
            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, true); // 근무 중 상태로 변경
            return true;
        } else {
            return false; // 출근 가능한 근무 없음
        }
    }

    /// 사용자의 실제 퇴근 시간을 기록합니다.
    /// @param userId 요청 사용자 ID
    /// @param workplaceId 근무지 ID
    @Transactional
    public void updateActualEndTime(Long userId, Long workplaceId) {
        // 권한 및 현재 근무 상태 확인
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);
        if (!Boolean.TRUE.equals(userWorker.getIsNowWorking())) { throw new WorkNotFoundException("현재 진행 중인 근무가 없습니다."); }

        // 가장 최근의 진행 중인 근무 조회
        Optional<Work> optWorkToEnd = workRepository.findMostRecentWorkInProgress(userWorker.getId());

        // 퇴근 기록 업데이트 및 상태 변경
        if (optWorkToEnd.isPresent()) {
            Work workToEnd = optWorkToEnd.get();
            LocalDateTime currentDateTime = LocalDateTime.now();

            // 예정된 종료 시간(null 포함)과 실제 종료 시간이 다르면 재계산
            boolean needsRecalculation = !Objects.equals(workToEnd.getEndTime(), currentDateTime);

            // 실제 퇴근 시간 업데이트 (예정 퇴근 시간이 없었다면 같이 업데이트)
            workRepository.updateActualEndTimeById(workToEnd.getId(), currentDateTime);

            // 급여 재계산 필요 여부 확인 및 실행
            if (needsRecalculation) {
                // 업데이트 후 work 객체를 다시 로드해서 정확한 정보로 재계산
                Work updatedWork = workRepository.findById(workToEnd.getId()).orElse(workToEnd); // DB 조회 실패 시 이전 객체 사용 (방어)
                salaryCalculationService.recalculateWorkWeek(userWorker.getId(), updatedWork.getWorkDate());
            }

            // 근무 중 상태 해제
            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, false);
        } else {
            // 비정상 상태 복구: 진행 중 근무가 없는데 isNowWorking=true인 경우
            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, false);
            log.warn("No work in progress found for workerId={}, but isNowWorking was true. Resetting isNowWorking.", userWorker.getId());
        }
    }

    // =================================================================
    // 근무 삭제 (Delete)
    // =================================================================

    /// '단일' 근무 기록을 삭제합니다.
    /// @param requesterUserId 요청 사용자 ID
    /// @param workId 삭제할 근무 ID
    @Transactional
    public void deleteWork(Long requesterUserId, Long workId) {
        // 권한 검증 및 관련 정보 로드
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);
        // 삭제 헬퍼 호출
        deleteWorkHelper(context.worker(), context.work());
    }

    /// 기준이 되는 반복 근무와 '반복' 근무 그룹의 '미래' 일정을 삭제합니다.
    /// @param requesterUserId 요청 사용자 ID
    /// @param workId 기준이 되는 반복 근무 그룹의 근무 ID
    @Transactional
    public void deleteRecurringWorkIncludingDate(Long requesterUserId, Long workId) {
        // 권한 검증 및 관련 정보 로드
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);
        Work work = context.work();
        // 반복 근무인지 확인
        if (work.getRepeatGroupId() == null) { throw new BadRequestException("반복 근무가 아닌 단일 근무입니다."); }

        // 해당 근무일 및 이후 모든 반복 일정 삭제
        long deletedCount = workRepository.deleteRecurringWorkFromDate(work.getRepeatGroupId(), work.getWorkDate());
        log.info("Deleted {} future recurring works for group {}", deletedCount, work.getRepeatGroupId());

        // 삭제된 주의 주휴수당 재계산 필요
        salaryCalculationService.recalculateWorkWeek(context.worker().getId(), work.getWorkDate());
    }

    // =================================================================
    // 내부 헬퍼 메서드 (Helpers)
    // =================================================================

    /// 사용자 근무 생성 헬퍼 (MyWorkCreateRequest 용)
    /// @return 생성된 근무 리스트 (단일 근무 시 크기 1)
    private List<Work> createMyWorkHelper(Worker worker, MyWorkCreateRequest request) {
        if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
            Work singleWork = createSingleWork(worker, request.getStartTime(), request.getEndTime(),
                    request.getActualStartTime(), request.getActualEndTime(),
                    request.getRestTimeMinutes(), request.getMemo());
            return Collections.singletonList(singleWork); // 단일 근무도 리스트로 반환
        } else {
            return createRecurringWorks(worker, request.getStartTime(), request.getEndTime(),
                    request.getRestTimeMinutes(), request.getMemo(),
                    request.getRepeatDays(), request.getRepeatEndDate());
        }
    }

    /// 사장님의 알바생 근무 생성 헬퍼 (WorkerWorkCreateRequest 용)
    /// @return 생성된 근무 리스트 (단일 근무 시 크기 1)
    private List<Work> createWorkForWorkerHelper(Worker worker, WorkerWorkCreateRequest request) {
        if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
            Work singleWork = createSingleWork(worker, request.getStartTime(), request.getEndTime(),
                    request.getActualStartTime(), request.getActualEndTime(),
                    request.getRestTimeMinutes(), request.getMemo());
            return Collections.singletonList(singleWork); // 단일 근무도 리스트로 반환
        } else {
            return createRecurringWorks(worker, request.getStartTime(), request.getEndTime(),
                    request.getRestTimeMinutes(), request.getMemo(),
                    request.getRepeatDays(), request.getRepeatEndDate());
        }
    }

    /// 단일 근무 생성 상세 로직
    private Work createSingleWork(Worker worker, LocalDateTime startTime, LocalDateTime endTime,
                                  LocalDateTime actualStartTime, LocalDateTime actualEndTime,
                                  Integer restTimeMinutes, String memo) {
        // 급여 정보 로드
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);
        int hourlyRate = salary.getHourlyRate() != null ? salary.getHourlyRate() : 0;
        boolean hasNightAllowance = salary.getHasNightAllowance();
        verifyStartEndTime(startTime, endTime); // 시간 유효성 검증

        // 일급 계산 (주휴수당 0으로)
        Work tempWork = Work.builder().startTime(startTime).endTime(endTime).restTimeMinutes(restTimeMinutes).hourlyRate(hourlyRate).build();
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(tempWork, 0, hasNightAllowance);

        // 최종 Work 엔티티 생성
        Work workToCreate = workWithDailyIncome.toBuilder()
                .workerId(worker.getId()).workDate(startTime.toLocalDate())
                .actualStartTime(actualStartTime).actualEndTime(actualEndTime) // 실제 시간 반영
                .memo(memo).repeatGroupId(null)
                .build();

        // DB 저장
        workRepository.create(workToCreate);
        // 주급 재계산 (주휴수당 반영 및 전파)
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workToCreate.getWorkDate());

        // 생성 및 재계산 후 최종 상태를 DB에서 다시 조회하여 반환
        return workRepository.findById(workToCreate.getId()).orElse(workToCreate);
    }

    /// 반복 근무 생성 상세 로직
    private List<Work> createRecurringWorks(Worker worker, LocalDateTime startTime, LocalDateTime endTime,
                                            Integer restTimeMinutes, String memo,
                                            List<DayOfWeek> repeatDays, LocalDate repeatEndDate) {
        // 급여 정보 로드 및 유효성 검증
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);
        int hourlyRate = salary.getHourlyRate() != null ? salary.getHourlyRate() : 0;
        boolean hasNightAllowance = salary.getHasNightAllowance();
        verifyStartEndTime(startTime, endTime);
        LocalDate startDate = startTime.toLocalDate();
        if (repeatEndDate == null || repeatEndDate.isBefore(startDate)) { throw new InvalidFieldFormatException("반복 종료 날짜는 시작 날짜 이후여야 합니다."); }
        if (ChronoUnit.DAYS.between(startDate, repeatEndDate) > MAX_REPEAT_DAYS_LIMIT) { throw new DataLimitExceedException("반복 기간은 최대 " + MAX_REPEAT_DAYS_LIMIT + "일까지 설정할 수 있습니다."); }

        // 반복 그룹 ID 생성 및 기본 일급 계산
        String repeatGroupId = UUID.randomUUID().toString();
        Work tempWork = Work.builder().startTime(startTime).endTime(endTime).restTimeMinutes(restTimeMinutes).hourlyRate(hourlyRate).build();
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(tempWork, 0, hasNightAllowance); // 주휴수당 0으로 일급 계산

        List<Work> worksToCreate = new ArrayList<>();
        Set<LocalDate> weeksToRecalculate = new HashSet<>(); // 주급 재계산 대상 주(월요일)
        LocalDate currentDate = startDate;
        long dayOffset = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate()); // 종료일이 다음날인지 확인

        // 반복 종료일까지 루프
        while (!currentDate.isAfter(repeatEndDate)) {
            // 해당 요일이 반복 요일에 포함되는 경우
            if (repeatDays.contains(currentDate.getDayOfWeek())) {
                LocalDateTime newStartTime = startTime.with(currentDate); // 날짜만 변경
                LocalDateTime newEndTime = endTime.with(currentDate.plusDays(dayOffset)); // 날짜 변경 (+ 다음날 여부)

                // Work 엔티티 생성 (일급 정보 재사용, 실제 시간은 null)
                Work recurringWork = workWithDailyIncome.toBuilder()
                        .id(null).workerId(worker.getId()).workDate(currentDate)
                        .startTime(newStartTime).endTime(newEndTime)
                        .actualStartTime(null) // 반복 생성 시 실제 시간 null
                        .actualEndTime(null)   // 반복 생성 시 실제 시간 null
                        .memo(memo).repeatGroupId(repeatGroupId) // 동일한 그룹 ID
                        .build();
                worksToCreate.add(recurringWork);
                weeksToRecalculate.add(currentDate.with(DayOfWeek.MONDAY)); // 해당 주의 월요일 추가
            }
            currentDate = currentDate.plusDays(1); // 다음 날짜로 이동
        }

        if (worksToCreate.isEmpty()) { throw new BadRequestException("반복 규칙에 해당하는 근무일이 없습니다."); }

        // DB에 배치 삽입
        workRepository.createBatch(worksToCreate);

        // 생성된 근무들의 주급 재계산 (주휴수당 반영)
        for (LocalDate weekStartDate : weeksToRecalculate) {
            salaryCalculationService.recalculateWorkWeek(worker.getId(), weekStartDate);
        }

        // 주급 재계산 후의 '최신' 상태를 반영하기 위해 다시 로드하여 반환
        // (주의: createBatch 후 ID가 없을 수 있으므로, group ID와 기간으로 조회)
        return workRepository.findAllByWorkerIdAndDateRange(
                        worker.getId(), startDate, repeatEndDate).stream()
                .filter(w -> Objects.equals(repeatGroupId, w.getRepeatGroupId()))
                .collect(Collectors.toList());
    }

    /// 반복 중단: 미래 반복 삭제 후 현재 근무는 단일로 업데이트
    private void stopRecurrenceAndUpdateSingle(Worker worker, Work currentWork, LocalDateTime newStartTime, LocalDateTime newEndTime,
                                               LocalDateTime newActualStartTime, LocalDateTime newActualEndTime,
                                               Integer newRestTimeMinutes, String newMemo) {
        // 1. 기존에 반복 그룹이 있었는지 확인
        if (currentWork.getRepeatGroupId() != null) {
            // 2. 현재 근무의 '다음 날'부터 미래 반복 삭제
            long deletedCount = workRepository.deleteRecurringWorkAfterDate(currentWork.getRepeatGroupId(), currentWork.getWorkDate());
            log.info("Stopped recurrence: Deleted {} future works after {} for group {}", deletedCount, currentWork.getWorkDate(), currentWork.getRepeatGroupId());
        }

        // 3. 현재 근무는 '단일' 근무로 업데이트 (repeatGroupId = null)
        updateSingleWorkInternal(worker, currentWork.getId(), newStartTime, newEndTime,
                newActualStartTime, newActualEndTime, newRestTimeMinutes, newMemo,
                null);
    }

    /// 새로운 반복 시작/변경: 기존 반복 삭제 후 새로운 반복 생성
    private List<Work> replaceWithNewRecurringWorks(Worker worker, Work currentWork, LocalDateTime newStartTime, LocalDateTime newEndTime,
                                                    Integer newRestTimeMinutes, String newMemo,
                                                    List<DayOfWeek> newRepeatDays, LocalDate newRepeatEndDate) {
        // 1. 기존에 반복 그룹이 있었는지 확인
        if (currentWork.getRepeatGroupId() != null) {
            // 2. 현재 근무 '포함'하여 미래 반복 삭제
            long deletedCount = workRepository.deleteRecurringWorkFromDate(currentWork.getRepeatGroupId(), currentWork.getWorkDate());
            log.info("Replacing recurrence: Deleted {} works from {} for group {}", deletedCount, currentWork.getWorkDate(), currentWork.getRepeatGroupId());
        } else {
            // 기존이 단일 근무였다면 해당 근무만 삭제
            workRepository.delete(currentWork.getId(), worker.getId());
            log.info("Replacing single work with recurrence: Deleted work id {}", currentWork.getId());
        }

        // 3. 새로운 반복 근무 생성 및 반환 (createRecurringWorks 헬퍼 재사용)
        // (주의: createRecurringWorks는 실제 시간(actual)을 null로 생성함)
        // - 주급 재계산은 createRecurringWorks 내부에서 처리됨
        return createRecurringWorks(worker, newStartTime, newEndTime,
                newRestTimeMinutes, newMemo, newRepeatDays, newRepeatEndDate);
    }

    /// '단일' 근무 업데이트 공통 로직
    private void updateSingleWorkInternal(Worker worker, Long workId, LocalDateTime startTime, LocalDateTime endTime,
                                          LocalDateTime actualStartTime, LocalDateTime actualEndTime,
                                          Integer restTimeMinutes, String memo,
                                          String repeatGroupId) {
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);
        int hourlyRate = salary.getHourlyRate() != null ? salary.getHourlyRate() : 0;
        boolean hasNightAllowance = salary.getHasNightAllowance();
        verifyStartEndTime(startTime, endTime);

        Work tempWork = Work.builder().startTime(startTime).endTime(endTime).restTimeMinutes(restTimeMinutes).hourlyRate(hourlyRate).build();
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(tempWork, 0, hasNightAllowance);

        Work workToUpdate = workWithDailyIncome.toBuilder()
                .id(workId)
                .workerId(worker.getId())
                .workDate(startTime.toLocalDate())
                .startTime(startTime).endTime(endTime)
                .actualStartTime(actualStartTime).actualEndTime(actualEndTime)
                .restTimeMinutes(restTimeMinutes).memo(memo)
                .repeatGroupId(repeatGroupId)
                .build();

        workRepository.update(workToUpdate);
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workToUpdate.getWorkDate());
    }

    /// '단일' 근무 삭제 헬퍼 (루틴 매핑 포함)
    private void deleteWorkHelper(Worker worker, Work work) {
        // 1. 연결된 루틴 매핑 먼저 삭제
        routineService.deleteWorkRoutineMappingByWorkId(work.getId());
        // 2. 근무 기록 삭제
        workRepository.delete(work.getId(), worker.getId());
        // 3. 해당 주의 주급 재계산 (주휴수당 조정)
        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());
    }

    /// 근무 조회 시 권한 검증 및 기본 정보 로드 헬퍼
    private VerifiedWorkContextForRead getVerifiedWorkContextForRead(Long requesterUserId, Long workId) {
        // 관련 엔티티 로드
        Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
        Worker requestedWorker = workerRepository.findById(work.getWorkerId()).orElseThrow(WorkerNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(requestedWorker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new);
        // 권한 검증
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, requestedWorker.getUserId(), workplace.getOwnerId());

        // 근무자의 User 정보 조회
        User workerUser = null;
        if (requestedWorker.getUserId() != null) {
            workerUser = userRepository.findById(requestedWorker.getUserId()).orElse(null); // 조회 실패 시 null
        }

        // 필요한 값 계산 및 DTO 생성
        long workMinutes = work.getNetWorkMinutes() != null ? work.getNetWorkMinutes() : 0; // 순수 근무 시간
        WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(requestedWorker, workerUser);
        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId()).workplaceName(workplace.getWorkplaceName()).isShared(workplace.isShared()).build();
        boolean isEditable = checkEditable(requesterUserId, requestedWorker.getUserId(), workplace.getOwnerId());
        // 결과 반환
        return new VerifiedWorkContextForRead(work, workMinutes, requestedWorker, workerSummaryInfo, workplaceSummary, isEditable);
    }

    /// 근무 수정/삭제 시 권한 검증 및 기본 정보 로드 헬퍼
    private VerifiedWorkContextForUD getVerifiedWorkContextForUD(Long requesterUserId, Long workId) {
        // 관련 엔티티 로드
        Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
        Worker worker = workerRepository.findById(work.getWorkerId()).orElseThrow(WorkerNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new);
        // 권한 검증
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, worker.getUserId(), workplace.getOwnerId());
        // 결과 반환
        return new VerifiedWorkContextForUD(work, worker);
    }

    /// 근무자 요약 DTO 생성 헬퍼 (User 정보 포함)
    private WorkerSummaryResponse createWorkerSummary(Worker worker, User user) {
        return WorkerSummaryResponse.builder()
                .workerId(worker.getId())
                .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                .nickname(user != null ? user.getNickname() : "탈퇴한 근무자")
                .profileImg(user != null ? user.getProfileImg() : null)
                .build();
    }

    /// 출/퇴근 시간 유효성 검증 헬퍼 (퇴근이 출근보다 빠르면 예외)
    private void verifyStartEndTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime != null && endTime.isBefore(startTime)) { throw new InvalidFieldFormatException("퇴근 시간은 출근 시간보다 미래여야 합니다."); }
    }

    /// Work 엔티티 -> WorkSummaryResponse DTO 변환 헬퍼 (반복 정보 변환 포함)
    /// 목록 조회 시에는 repeatInfoCache를 전달하여 N+1 방지
    private WorkSummaryResponse convertWorkToSummaryResponse(
            Work work, WorkerSummaryResponse workerSummaryInfo, WorkplaceSummaryResponse workplaceSummaryInfo,
            long workMinutes, boolean isMyWork, boolean isEditable, Map<String, RepeatInfo> repeatInfoCache) { // ⬅️ 캐시 파라미터 추가

        List<DayOfWeek> repeatDays = Collections.emptyList();
        LocalDate repeatEndDate = null;

        // 반복 근무인 경우
        if (work.getRepeatGroupId() != null) {
            RepeatInfo repeatInfo = repeatInfoCache.get(work.getRepeatGroupId());
            // 캐시에 없으면 DB에서 조회 (캐시 미사용 시나리오 대비)
            if (repeatInfo == null) {
                log.warn("RepeatInfo not found in cache for group ID: {}. Querying DB.", work.getRepeatGroupId()); // 캐시 누락 시 로그
                repeatInfo = getRepeatInfo(work.getRepeatGroupId());
            }
            repeatDays = repeatInfo.days();
            repeatEndDate = repeatInfo.endDate();
        }

        return WorkSummaryResponse.builder()
                .workId(work.getId())
                .workerSummaryInfo(workerSummaryInfo)
                .workplaceSummaryInfo(workplaceSummaryInfo)
                .workDate(work.getWorkDate())
                .startTime(work.getStartTime())
                .endTime(work.getEndTime())
                .workMinutes(workMinutes)
                .restTimeMinutes(work.getRestTimeMinutes())
                .estimatedNetIncome(work.getEstimatedNetIncome())
                .repeatDays(repeatDays)
                .repeatEndDate(repeatEndDate)
                .isMyWork(isMyWork)
                .isEditable(isEditable)
                .build();
    }

    /// 반복 그룹 ID로 반복 요일과 종료일을 조회하는 헬퍼
    private RepeatInfo getRepeatInfo(String repeatGroupId) {
        // 1. 반복 종료일 조회
        LocalDate endDate = workRepository.findLastWorkDateByRepeatGroupId(repeatGroupId)
                .orElse(null); // 종료일이 없는 경우는 없어야 하지만 방어 코드

        // 2. 반복 요일 조회 (DB 함수 결과 문자열 -> DayOfWeek Enum 변환)
        List<String> dayNames = workRepository.findDistinctDayNamesByRepeatGroupId(repeatGroupId);
        List<DayOfWeek> daysOfWeek = dayNames.stream()
                .map(this::dayNameToDayOfWeek) // 대소문자 무시 변환
                .filter(Objects::nonNull)      // 변환 실패 시 null 필터링
                .sorted()                      // 요일 순서 정렬 (Optional)
                .collect(Collectors.toList());

        return new RepeatInfo(daysOfWeek, endDate);
    }

    /// DB에서 반환된 요일 이름 문자열을 DayOfWeek Enum으로 변환하는 헬퍼
    private DayOfWeek dayNameToDayOfWeek(String dayName) {
        if (dayName == null) return null;
        try {
            return DayOfWeek.valueOf(dayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert day name to DayOfWeek: {}", dayName, e);
            return null; // 변환 실패 시 null 반환
        }
    }

    /// 특정 기간 동안 주어진 근무자 ID 목록에 대한 근무 기록과 반복 정보를 미리 로드합니다.
    /// @param workerIdList 조회할 Worker ID 목록
    /// @param startDate 조회 시작일
    /// @param endDate 조회 종료일
    /// @return 미리 로드된 근무 데이터 및 반복 정보 캐시 (CalendarWorkData)
    private CalendarWorkData preloadCalendarWorkData(List<Long> workerIdList, LocalDate startDate, LocalDate endDate) {
        // 1. Work 정보 한 번에 조회
        List<Work> allWorks = workRepository.findAllByWorkerIdListInAndDateRange(workerIdList, startDate, endDate);

        // 2. Work 정보 Map으로 변환 (workerId를 key로 그룹화)
        Map<Long, List<Work>> workMapByWorker = allWorks.stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // 3. 반복 정보 캐시 생성
        Map<String, RepeatInfo> repeatInfoCache = prefetchRepeatInfo(allWorks);

        // 4. 결과 반환
        return new CalendarWorkData(allWorks, workMapByWorker, repeatInfoCache);
    }

    /// 근무 목록에서 반복 그룹 ID를 추출하고, 최적화된 쿼리로 반복 정보를 미리 조회하여 캐시를 생성합니다.
    private Map<String, RepeatInfo> prefetchRepeatInfo(List<Work> works) {
        // 1. 목록에서 고유한 repeatGroupId 추출
        Set<String> repeatGroupIds = works.stream()
                .map(Work::getRepeatGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (repeatGroupIds.isEmpty()) {
            return Collections.emptyMap(); // 반복 근무가 없으면 빈 맵 반환
        }

        // 2. [쿼리 1] 모든 그룹의 마지막 날짜(endDate)를 한 번에 조회
        Map<String, LocalDate> endDateMap = workRepository.findLastWorkDatesByGroupIdList(repeatGroupIds)
                .stream()
                .collect(Collectors.toMap(WorkRepository.GroupIdAndDate::groupId,
                        WorkRepository.GroupIdAndDate::lastDate));

        // 3. [쿼리 2] 모든 그룹의 (그룹 ID, 요일 이름) 쌍을 한 번에 조회
        List<WorkRepository.GroupIdAndDayName> dayNamePairs = workRepository.findDistinctDayNamesByGroupIdList(repeatGroupIds);

        // 4. (그룹 ID, 요일 이름) 쌍 리스트를 -> Map<그룹 ID, List<DayOfWeek>> 로 변환
        Map<String, List<DayOfWeek>> daysMap = new HashMap<>();
        for (WorkRepository.GroupIdAndDayName pair : dayNamePairs) {
            DayOfWeek dayOfWeek = dayNameToDayOfWeek(pair.dayName());
            if (dayOfWeek != null) {
                // 해당 groupId의 리스트를 가져오거나 새로 만들어서 요일 추가
                daysMap.computeIfAbsent(pair.groupId(), k -> new ArrayList<>()).add(dayOfWeek);
            }
        }
        // (선택적) 각 그룹의 요일 리스트 정렬
        daysMap.values().forEach(Collections::sort);

        // 5. 최종 캐시 Map 생성 (endDateMap과 daysMap 조합)
        Map<String, RepeatInfo> cache = new HashMap<>();
        for (String groupId : repeatGroupIds) {
            LocalDate endDate = endDateMap.get(groupId); // null일 수 없음 (이론상)
            List<DayOfWeek> days = daysMap.getOrDefault(groupId, Collections.emptyList()); // 요일이 없을 수 있음 (DB 데이터 오류 시)
            if (endDate != null) { // endDate가 null이면 캐시에 넣지 않음 (방어 코드)
                cache.put(groupId, new RepeatInfo(days, endDate));
            } else {
                log.error("Could not find end date for repeat group ID: {}", groupId); // 종료일 누락 시 에러 로그
            }
        }

        return cache;
    }

    /// 요청자가 해당 근무의 주체인지 확인하는 헬퍼
    private boolean checkIsMyWork(Long requesterUserId, Long workerUserId) {
        return Objects.equals(requesterUserId, workerUserId);
    }

    /// 요청자가 해당 근무를 수정/삭제할 권한이 있는지 확인하는 헬퍼 (본인 또는 사장님)
    private boolean checkEditable(Long userId, Long workerUserId, Long workplaceOwnerId) {
        return (workerUserId != null && workerUserId.equals(userId)) // 본인 근무자인 경우 (null 체크 포함)
                || Objects.equals(workplaceOwnerId, userId);       // 또는 근무지 사장님인 경우 (null 안전 비교)
    }
}