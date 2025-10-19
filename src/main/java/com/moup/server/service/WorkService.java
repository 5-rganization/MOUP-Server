package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.repository.*;
import com.moup.server.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

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

    private record VerifiedWorkContextForCreate(
            Work work,
            long workMinutes,
            WorkerSummaryResponse workerSummaryInfo,
            WorkplaceSummaryResponse workplaceSummaryInfo,
            boolean isEditable
    ) {}

    private record VerifiedWorkContextForUpdate(Work work, Worker worker) {}

    @Transactional
    public WorkCreateResponse createMyWork(Long userId, Long workplaceId, WorkCreateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        Work work = createWorkHelper(userWorker, request);

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional
    public WorkCreateResponse createWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, WorkCreateRequest request) {
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(requesterUserId, workplaceOwnerId);

        Work work = createWorkHelper(worker, request);

        Long workerUserId = userRepository.findById(worker.getUserId()).orElseThrow(UserNotFoundException::new).getId();
        routineService.saveWorkRoutineMapping(workerUserId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workId) {
        VerifiedWorkContextForCreate context = getVerifiedWorkContextForCreate(userId, workId);

        List<RoutineSummaryResponse> routineSummaryList = routineService.getAllSummarizedRoutineByWorkRoutineMapping(userId, workId);

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
    public WorkSummaryResponse getSummarizedWork(Long userId, Long workId) {
        VerifiedWorkContextForCreate context = getVerifiedWorkContextForCreate(userId, workId);

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
                .repeatDays(repeatDays)
                .repeatEndDate(context.work().getRepeatEndDate())
                .isEditable(context.isEditable())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkCalendarListResponse getAllWork(Long userId, YearMonth baseYearMonth) {
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
                        long workMinutes = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();
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

        // 1. 매장 정보를 조회하고, 사용자가 사장님인지 확인합니다.
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);

        permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplace.getOwnerId());

        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
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
            User workerUser = userMap.get(workplaceWorker.getUserId());
            if (workerUser == null) continue;

            WorkerSummaryResponse workerSummaryInfo = WorkerSummaryResponse.builder()
                    .workerId(workplaceWorker.getId())
                    .workerBasedLabelColor(workplaceWorker.getWorkerBasedLabelColor())
                    .ownerBasedLabelColor(workplaceWorker.getOwnerBasedLabelColor())
                    .nickname(workerUser.getNickname())
                    .profileImg(workerUser.getProfileImg())
                    .build();

            List<Work> workerWorkList = workMapByWorker.getOrDefault(workplaceWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workerWorkSummaryList = workerWorkList.stream()
                    .map(work -> {
                        long workMinutes = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();
                        // 현재 사용자가 사장님이므로 모든 근무를 수정 가능
                        boolean isEditable = checkEditable(user.getId(), workplaceWorker.getUserId(), workplace.getOwnerId());
                        return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummary, workMinutes, isEditable);
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
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
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
                    long workMinutes = Duration.between(userWork.getStartTime(), userWork.getEndTime()).toMinutes();
                    // 사용자가 자신의 근무이거나, 또는 사용자가 사장님일 경우 수정 가능
                    boolean isEditable = checkEditable(user.getId(), userWorker.getUserId(), workplace.getOwnerId());
                    return convertWorkToSummaryResponse(userWork, userWorkerSummaryInfo, workplaceSummary, workMinutes, isEditable);
                })
                .toList();

        return WorkCalendarListResponse.builder()
                .workSummaryInfoList(workSummaryInfoList)
                .build();
    }

    @Transactional
    public void updateWork(Long requesterUserId, Long workId, WorkUpdateRequest request) {
        VerifiedWorkContextForUpdate context = getVerifiedWorkContextForUpdate(requesterUserId, workId);

        updateWorkHelper(context.worker(), workId, request);

        routineService.saveWorkRoutineMapping(context.worker().getUserId(), request.getRoutineIdList(), workId);
    }

    @Transactional
    public void deleteWork(Long requesterUserId, Long workId) {
        VerifiedWorkContextForUpdate context = getVerifiedWorkContextForUpdate(requesterUserId, workId);

        deleteWorkHelper(context.worker(), context.work());
    }

    private Work createWorkHelper(Worker worker, WorkCreateRequest request) {
        int hourlyRate = salaryRepository.findByWorkerId(worker.getId())
                .map(Salary::getHourlyRate)
                .orElse(0);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        Work work = request.toEntity(worker.getId(), hourlyRate);
        workRepository.create(work);

        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());

        return work;
    }

    private void updateWorkHelper(Worker worker, Long workId, WorkUpdateRequest request) {
        int hourlyRate = salaryRepository.findByWorkerId(worker.getId())
                .map(Salary::getHourlyRate)
                .orElse(0);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        Work work = request.toEntity(workId, worker.getId(), hourlyRate);
        workRepository.update(work);

        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());
    }

    private void deleteWorkHelper(Worker worker, Work work) {
        workRepository.delete(work.getId(), worker.getId());

        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());
    }

    private VerifiedWorkContextForCreate getVerifiedWorkContextForCreate(Long requesterUserId, Long workId) {
        // 1. (쿼리 1) workId로 Work 정보 조회
        Work work = workRepository.findById(workId)
                .orElseThrow(WorkNotFoundException::new);

        // 2. (쿼리 2) Work에서 workerId를 가져와 Worker 정보 조회
        Worker requestedWorker = workerRepository.findById(work.getWorkerId())
                .orElseThrow(WorkerWorkplaceNotFoundException::new);

        // 3. (쿼리 3) Worker에서 workplaceId를 가져와 Workplace 정보 조회
        Workplace workplace = workplaceRepository.findById(requestedWorker.getWorkplaceId())
                .orElseThrow(WorkplaceNotFoundException::new);

        // 4. 권한 검사: 요청자가 근무자 본인이거나 근무지 사장님인지 확인
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, requestedWorker.getUserId(), workplace.getOwnerId());

        // 5. 근무 시간 계산
        long workMinutes = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();

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
        return new VerifiedWorkContextForCreate(work, workMinutes, workerSummaryInfo, workplaceSummary, isEditable);
    }

    private VerifiedWorkContextForUpdate getVerifiedWorkContextForUpdate(Long requesterUserId, Long workId) {
        // 1. (쿼리 1) workId로 Work 정보 조회
        Work work = workRepository.findById(workId)
                .orElseThrow(WorkNotFoundException::new);

        // 2. (쿼리 2) Work에서 workerId를 가져와 Worker 정보 조회
        Worker worker = workerRepository.findById(work.getWorkerId())
                .orElseThrow(WorkerWorkplaceNotFoundException::new);

        // 3. (쿼리 3) Worker에서 workplaceId를 가져와 Workplace 정보 조회
        Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId())
                .orElseThrow(WorkplaceNotFoundException::new);

        // 4. 권한 검사: 요청자가 근무자 본인이거나 근무지 사장님인지 확인
        permissionVerifyUtil.verifyWorkerPermission(requesterUserId, worker.getUserId(), workplace.getOwnerId());

        return new VerifiedWorkContextForUpdate(work, worker);
    }

    private WorkerSummaryResponse createWorkerSummary(Worker worker) {
        User user = userRepository.findById(worker.getUserId())
                .orElseThrow(UserNotFoundException::new);

        return WorkerSummaryResponse.builder()
                .workerId(worker.getId())
                .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .build();
    }

    private void verifyStartEndTime(LocalDateTime startTime, LocalDateTime endTime) {
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
                .repeatDays(repeatDays)
                .repeatEndDate(work.getRepeatEndDate())
                .isEditable(isEditable)
                .build();
    }

    private boolean checkEditable(Long userId, Long workerUserId, Long workplaceOwnerId) {
        return workerUserId.equals(userId) || workplaceOwnerId.equals(userId);
    }
}
