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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;
    private final UserRepository userRepository;

    private final RoutineService routineService;
    private final SalaryCalculationService salaryCalculationService;

    private final PermissionVerifyUtil permissionVerifyUtil;

    private record VerifiedWorkContextForRead(
            Work work,
            long workMinutes,
            WorkerSummaryResponse workerSummaryInfo,
            WorkplaceSummaryResponse workplaceSummaryInfo,
            boolean isEditable
    ) {}

    private record VerifiedWorkContextForUD(Work work, Worker worker) {}

    @Transactional
    public WorkCreateResponse createMyWork(Long userId, Long workplaceId, MyWorkCreateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        Work work = createMyWorkHelper(userWorker, request);

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional
    public WorkCreateResponse createWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, WorkerWorkCreateRequest request) {
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new);
        if (worker.getUserId() == null) { throw new WorkerNotFoundException("이미 탈퇴한 근무자입니다."); }
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(requesterUserId, workplaceOwnerId);

        Work work = createWorkForWorkerHelper(worker, request);

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workId) {
        VerifiedWorkContextForRead context = getVerifiedWorkContextForRead(userId, workId);

        List<RoutineSummaryResponse> routineSummaryList = routineService.getAllRoutineByWorkRoutineMapping(userId, workId);

        List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(context.work().getRepeatDays());

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
                .repeatEndDate(context.work().getRepeatEndDate())
                .isEditable(context.isEditable())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkSummaryResponse getWork(Long userId, Long workId) {
        VerifiedWorkContextForRead context = getVerifiedWorkContextForRead(userId, workId);

        List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(context.work().getRepeatDays());

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
                .repeatEndDate(context.work().getRepeatEndDate())
                .isEditable(context.isEditable())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkCalendarListResponse getAllMyWork(Long userId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // 1. 사용자의 모든 Worker 정보 조회 (쿼리 1)
        List<Worker> userWorkerList = workerRepository.findAllByUserId(userId);
        if (userWorkerList.isEmpty()) {
            return WorkCalendarListResponse.builder()
                    .workSummaryInfoList(Collections.emptyList())
                    .build();
        }

        // 2. 사용자 정보 조회 (쿼리 2)
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 3. ID 리스트 추출
        List<Long> workplaceIdList = userWorkerList.stream()
                .map(Worker::getWorkplaceId)
                .distinct()
                .toList();
        List<Long> workerIdList = userWorkerList.stream()
                .map(Worker::getId)
                .toList();

        // 4. Workplace 정보 한 번에 조회 (쿼리 3) 및 Map으로 변환
        Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdListIn(workplaceIdList).stream()
                .collect(Collectors.toMap(Workplace::getId, workplace -> workplace));

        // 5. Work 정보 한 번에 조회 (쿼리 4) 및 Map으로 변환 (workerId를 key로)
        List<Work> allWorks = workRepository.findAllByWorkerIdListInAndDateRange(workerIdList, startDate, endDate);
        Map<Long, List<Work>> workMapByWorker = allWorks.stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // 6. DTO 조립 (추가 쿼리 없음)
        List<WorkSummaryResponse> userWorkSummaryList = new ArrayList<>();
        for (Worker userWorker : userWorkerList) {
            Workplace workplace = workplaceMap.get(userWorker.getWorkplaceId());
            // workplace가 null인 경우 방어 코드 (데이터 정합성이 깨졌을 경우)
            if (workplace == null) continue;

            permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplace.getOwnerId());

            WorkerSummaryResponse workerSummaryInfo = WorkerSummaryResponse.builder()
                    .workerId(userWorker.getId())
                    .workerBasedLabelColor(userWorker.getWorkerBasedLabelColor())
                    .ownerBasedLabelColor(userWorker.getOwnerBasedLabelColor())
                    .nickname(user.getNickname())
                    .profileImg(user.getProfileImg())
                    .build();

            WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            List<Work> workerWorkList = workMapByWorker.getOrDefault(userWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workSummaryList = workerWorkList.stream()
                    .map(work -> {
                        long workMinutes = work.getGrossWorkMinutes();
                        boolean isEditable = checkEditable(userId, userWorker.getUserId(), workplace.getOwnerId());
                        return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummaryInfo, workMinutes, isEditable);
                    })
                    .toList();
            userWorkSummaryList.addAll(workSummaryList);
        }

        return WorkCalendarListResponse.builder()
                .workSummaryInfoList(userWorkSummaryList)
                .build();
    }

    @Transactional(readOnly = true)
    public WorkCalendarListResponse getAllWorkByWorkplace(User user, Long workplaceId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // 1. 근무지(매장) 정보를 조회하고, 사용자가 근무자인지 확인합니다.
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);

        Long workerUserId = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerNotFoundException::new).getUserId();

        permissionVerifyUtil.verifyWorkerPermission(user.getId(), workerUserId, workplace.getOwnerId());

        WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        List<WorkSummaryResponse> workSummaryInfoList = new ArrayList<>();

        // 2. 사업장의 모든 Worker 정보 조회 (쿼리 1)
        List<Worker> workplaceWorkerList = workerRepository.findAllByWorkplaceId(workplaceId);

        if (workplaceWorkerList.isEmpty()) {
            return WorkCalendarListResponse.builder()
                    .workSummaryInfoList(Collections.emptyList())
                    .build();
        }

        // 3. ID 리스트 추출
        List<Long> workerIdList = workplaceWorkerList.stream()
                .map(Worker::getId)
                .toList();
        List<Long> userIdList = workplaceWorkerList.stream()
                .map(Worker::getUserId).distinct()
                .toList();

        // 4. User 정보 한 번에 조회 (쿼리 2) 및 Map 변환
        Map<Long, User> userMap = userRepository.findAllByIdListIn(userIdList).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 5. Work 정보 한 번에 조회 (쿼리 3) 및 Map 변환
        List<Work> allWorkList = workRepository.findAllByWorkerIdListInAndDateRange(workerIdList, startDate, endDate);
        Map<Long, List<Work>> workMapByWorker = allWorkList.stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // 6. DTO 조립 (추가 쿼리 없음)
        for (Worker workplaceWorker : workplaceWorkerList) {
            User workerUser;
            if (workplaceWorker.getUserId() != null) {
                workerUser = userMap.get(workplaceWorker.getUserId());
            } else {
                workerUser = null;
            }

            WorkerSummaryResponse workerSummaryInfo = WorkerSummaryResponse.builder()
                    .workerId(workplaceWorker.getId())
                    .workerBasedLabelColor(workplaceWorker.getWorkerBasedLabelColor())
                    .ownerBasedLabelColor(workplaceWorker.getOwnerBasedLabelColor())
                    .nickname(workerUser != null ? workerUser.getNickname() : "탈퇴한 근무자")
                    .profileImg(workerUser != null ? workerUser.getProfileImg() : null)
                    .build();

            List<Work> workerWorkList = workMapByWorker.getOrDefault(workplaceWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workerWorkSummaryList = workerWorkList.stream()
                    .map(work -> {
                        long workMinutes = work.getGrossWorkMinutes();
                        boolean isEditable = checkEditable(user.getId(), workplaceWorker.getUserId(), workplace.getOwnerId());
                        return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummaryInfo, workMinutes, isEditable);
                    })
                    .toList();
            workSummaryInfoList.addAll(workerWorkSummaryList);
        }

        return WorkCalendarListResponse.builder()
                .workSummaryInfoList(workSummaryInfoList)
                .build();
    }

    public WorkCalendarListResponse getAllMyWorkByWorkplace(User user, Long workplaceId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // 1. 사용자의 Worker 정보 조회
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId)
                .orElseThrow(WorkerNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);

        // 2. Workplace 요약 정보 생성
        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        // 3. 사용자 Worker 요약 정보 생성
        WorkerSummaryResponse userWorkerSummaryInfo = WorkerSummaryResponse.builder()
                .workerId(userWorker.getId())
                .workerBasedLabelColor(userWorker.getWorkerBasedLabelColor())
                .ownerBasedLabelColor(userWorker.getOwnerBasedLabelColor())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .build();

        // 4. 사용자의 근무 기록만 조회
        List<Work> userWorkList = workRepository.findAllByWorkerIdAndDateRange(userWorker.getId(), startDate, endDate);

        // 5. DTO로 변환
        List<WorkSummaryResponse> workSummaryInfoList = userWorkList.stream()
                .map(userWork -> {
                    long workMinutes = userWork.getGrossWorkMinutes();
                    return convertWorkToSummaryResponse(userWork, userWorkerSummaryInfo, workplaceSummary, workMinutes, true);
                })
                .toList();

        return WorkCalendarListResponse.builder()
                .workSummaryInfoList(workSummaryInfoList)
                .build();
    }

    @Transactional
    public void updateMyWork(Long requesterUserId, Long workId, MyWorkUpdateRequest request) {
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);

        updateMyWorkHelper(context.worker(), workId, request);

        routineService.saveWorkRoutineMapping(context.worker().getUserId(), request.getRoutineIdList(), workId);
    }

    @Transactional
    public void updateWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, Long workId, WorkerWorkUpdateRequest request) {
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);
        Worker workerOfWork = context.worker();

        if (!workerOfWork.getWorkplaceId().equals(workplaceId)) { throw new InvalidPermissionAccessException(); }
        if (!workerOfWork.getId().equals(workerId)) { throw new InvalidPermissionAccessException(); }

        updateWorkForWorkerHelper(workerOfWork, workId, request);
    }

    @Transactional
    public boolean updateActualStartTime(Long userId, Long workplaceId) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        if (workerRepository.existsByUserIdAndIsNowWorking(userId, true)) {
            throw new WorkerAlreadyWorkingException();
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        Optional<Work> optWorkToStart = workRepository.findEligibleWorkForClockIn(userWorker.getId(), currentDateTime);

        if (optWorkToStart.isPresent()) {
            Work workToStart = optWorkToStart.get();
            workRepository.updateActualStartTimeById(workToStart.getId(), currentDateTime);
            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, true);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public void updateActualEndTime(Long userId, Long workplaceId) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        if (userWorker.getIsNowWorking() == null || Boolean.FALSE.equals(userWorker.getIsNowWorking())) {
            throw new WorkNotFoundException();
        }

        Optional<Work> optWorkToEnd = workRepository.findMostRecentWorkInProgress(userWorker.getId());

        if (optWorkToEnd.isPresent()) {
            Work workToEnd = optWorkToEnd.get();
            boolean needsRecalculation = (workToEnd.getEndTime() == null);

            workRepository.updateActualEndTimeById(workToEnd.getId(), LocalDateTime.now());
            if (needsRecalculation) {
                salaryCalculationService.recalculateWorkWeek(userWorker.getId(), workToEnd.getWorkDate());
            }

            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, false);
        } else {
            workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, false);
            log.error("There is no work in progress: workerId = {}", userWorker.getId());
        }
    }

    @Transactional
    public void deleteWork(Long requesterUserId, Long workId) {
        VerifiedWorkContextForUD context = getVerifiedWorkContextForUD(requesterUserId, workId);

        deleteWorkHelper(context.worker(), context.work());
    }

    /// 사용자 근무 생성 헬퍼
    private Work createMyWorkHelper(Worker worker, MyWorkCreateRequest request) {
        // 1. 급여 정보 조회 (기존 로직과 동일)
        Optional<Salary> optSalary = salaryRepository.findByWorkerId(worker.getId());
        int hourlyRate = optSalary.map(Salary::getHourlyRate).orElse(0);
        boolean hasNightAllowance = optSalary.map(Salary::getHasNightAllowance).orElse(false);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        // 2. DTO -> Entity 변환 (급여 필드는 모두 0으로 초기화)
        Work work = request.toEntity(
                worker.getId(),
                hourlyRate, 0, 0, 0, 0, 0, 0
        );

        // 3. 일급 계산 (주휴수당 0으로)
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(work, 0, hasNightAllowance);

        // 4. DB 생성
        workRepository.create(workWithDailyIncome);

        // 5. 주급 재계산 (주휴수당 포함)
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workWithDailyIncome.getWorkDate());

        // 6. 생성된 Work 객체 반환
        return workWithDailyIncome;
    }

    /// 사장님의 알바생 근무 생성 헬퍼
    private Work createWorkForWorkerHelper(Worker worker, WorkerWorkCreateRequest request) {
        // 1. 급여 정보 조회 (기존 로직과 동일)
        Optional<Salary> optSalary = salaryRepository.findByWorkerId(worker.getId());
        int hourlyRate = optSalary.map(Salary::getHourlyRate).orElse(0);
        boolean hasNightAllowance = optSalary.map(Salary::getHasNightAllowance).orElse(false);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        // 2. DTO -> Entity 변환 (급여 필드는 모두 0으로 초기화)
        Work work = request.toEntity(
                worker.getId(),
                hourlyRate, 0, 0, 0, 0, 0, 0
        );

        // 3. 일급 계산 (주휴수당 0으로)
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(work, 0, hasNightAllowance);

        // 4. DB 생성
        workRepository.create(workWithDailyIncome);

        // 5. 주급 재계산 (주휴수당 포함)
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workWithDailyIncome.getWorkDate());

        // 6. 생성된 Work 객체 반환
        return workWithDailyIncome;
    }

    /// 사용자 근무 업데이트 헬퍼
    private void updateMyWorkHelper(Worker worker, Long workId, MyWorkUpdateRequest request) {
        Optional<Salary> optSalary = salaryRepository.findByWorkerId(worker.getId());

        int hourlyRate = optSalary.map(Salary::getHourlyRate).orElse(0);
        boolean hasNightAllowance = optSalary.map(Salary::getHasNightAllowance).orElse(false);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        // 1. DTO -> Entity 변환 (급여 필드는 모두 0으로 초기화)
        Work work = request.toEntity(
                workId,
                worker.getId(),
                hourlyRate,
                0,
                0,
                0,
                0,
                0,
                0
        );

        // 2. SalaryCalculationService를 호출하여 '일급' 계산
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(work, 0, hasNightAllowance);

        // 3. '일급'이 계산된 Work 객체를 DB에 업데이트
        workRepository.update(workWithDailyIncome);

        // 4. '주휴수당'을 포함한 '주급' 재계산
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workWithDailyIncome.getWorkDate());
    }

    /// 사장님의 알바생 근무 업데이트 헬퍼
    private void updateWorkForWorkerHelper(Worker worker, Long workId, WorkerWorkUpdateRequest request) {
        Optional<Salary> optSalary = salaryRepository.findByWorkerId(worker.getId());

        int hourlyRate = optSalary.map(Salary::getHourlyRate).orElse(0);
        boolean hasNightAllowance = optSalary.map(Salary::getHasNightAllowance).orElse(false);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        // 1. DTO -> Entity 변환 (급여 필드는 모두 0으로 초기화)
        Work work = request.toEntity(
                workId,
                worker.getId(),
                hourlyRate,
                0,
                0,
                0,
                0,
                0,
                0
        );

        // 2. SalaryCalculationService를 호출하여 '일급' 계산
        Work workWithDailyIncome = salaryCalculationService.calculateDailyIncome(work, 0, hasNightAllowance);

        // 3. '일급'이 계산된 Work 객체를 DB에 업데이트
        workRepository.update(workWithDailyIncome);

        // 4. '주휴수당'을 포함한 '주급' 재계산
        salaryCalculationService.recalculateWorkWeek(worker.getId(), workWithDailyIncome.getWorkDate());
    }

    private void deleteWorkHelper(Worker worker, Work work) {
        workRepository.delete(work.getId(), worker.getId());

        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());
    }

    private VerifiedWorkContextForRead getVerifiedWorkContextForRead(Long requesterUserId, Long workId) {
        // 1. (쿼리 1) workId로 Work 정보 조회
        Work work = workRepository.findById(workId)
                .orElseThrow(WorkNotFoundException::new);

        // 2. (쿼리 2) Work에서 workerId를 가져와 Worker 정보 조회
        Worker requestedWorker = workerRepository.findById(work.getWorkerId())
                .orElseThrow(WorkerNotFoundException::new);

        // 3. (쿼리 3) Worker에서 workplaceId를 가져와 Workplace 정보 조회
        Workplace workplace = workplaceRepository.findById(requestedWorker.getWorkplaceId())
                .orElseThrow(WorkplaceNotFoundException::new);

        // 4. 권한 검사: 요청자가 근무자 본인이거나 근무지 사장님인지 확인
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, requestedWorker.getUserId(), workplace.getOwnerId());

        // 5. 근무 시간 계산
        long workMinutes = work.getGrossWorkMinutes();

        // 6. 근무자 요약 DTO 생성
        WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(requestedWorker);

        // 7. 근무지 요약 DTO 생성
        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        // 8. 수정 가능 여부 계산
        boolean isEditable = checkEditable(requesterUserId, requestedWorker.getUserId(), workplace.getOwnerId());

        // 9. 모든 데이터를 컨테이너에 담아 반환
        return new VerifiedWorkContextForRead(work, workMinutes, workerSummaryInfo, workplaceSummary, isEditable);
    }

    private VerifiedWorkContextForUD getVerifiedWorkContextForUD(Long requesterUserId, Long workId) {
        // 1. (쿼리 1) workId로 Work 정보 조회
        Work work = workRepository.findById(workId)
                .orElseThrow(WorkNotFoundException::new);

        // 2. (쿼리 2) Work에서 workerId를 가져와 Worker 정보 조회
        Worker worker = workerRepository.findById(work.getWorkerId())
                .orElseThrow(WorkerNotFoundException::new);

        // 3. (쿼리 3) Worker에서 workplaceId를 가져와 Workplace 정보 조회
        Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId())
                .orElseThrow(WorkplaceNotFoundException::new);

        // 4. 권한 검사: 요청자가 근무자 본인이거나 근무지 사장님인지 확인
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, worker.getUserId(), workplace.getOwnerId());

        return new VerifiedWorkContextForUD(work, worker);
    }

    private WorkerSummaryResponse createWorkerSummary(Worker worker) {
        User user = null;
        if (worker.getUserId() != null) {
            user = userRepository.findById(worker.getUserId()).orElseThrow(WorkerNotFoundException::new);
        }

        return WorkerSummaryResponse.builder()
                .workerId(worker.getId())
                .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                .nickname(user != null ? user.getNickname() : "탈퇴한 근무자")
                .profileImg(user != null ? user.getProfileImg() : null)
                .build();
    }

    private void verifyStartEndTime(LocalDateTime startTime, LocalDateTime endTime) {
        // endTime이 null인 경우는 '진행 중'이므로 유효성 검사 통과
        if (endTime == null) { return; }
        if (endTime.isBefore(startTime)) { throw new InvalidFieldFormatException("퇴근 시간은 출근 시간보다 미래여야 합니다."); }
    }

    private List<DayOfWeek> convertDayOfWeekStrToList(String repeatDaysStr) {
        if (repeatDaysStr == null || repeatDaysStr.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(repeatDaysStr.split(","))
                    .map(String::trim)
                    .map(DayOfWeek::valueOf)
                    .toList();
        }
    }

    private WorkSummaryResponse convertWorkToSummaryResponse(
            Work work,
            WorkerSummaryResponse workerSummaryInfo,
            WorkplaceSummaryResponse workplaceSummaryInfo,
            long workMinutes,
            boolean isEditable
    ) {
        List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(work.getRepeatDays());

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
                .repeatEndDate(work.getRepeatEndDate())
                .isEditable(isEditable)
                .build();
    }

    private boolean checkEditable(Long userId, Long workerUserId, Long workplaceOwnerId) {
        return workerUserId.equals(userId) || workplaceOwnerId.equals(userId);
    }
}
