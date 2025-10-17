package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.*;
import com.moup.server.repository.*;
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

    private record VerifiedWorkContext(
            Work work,
            long workMinutes,
            WorkerSummaryResponse workerSummaryInfo,
            WorkplaceSummaryResponse workplaceSummaryInfo,
            boolean isEditable
    ) {}

    @Transactional
    public WorkCreateResponse createMyWork(Long userId, Long workplaceId, WorkCreateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(userId, userWorker.getUserId(), workplaceOwnerId);

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
        verifyPermission(requesterUserId, worker.getUserId(), workplaceOwnerId);

        Work work = createWorkHelper(worker, request);

        Long workerUserId = userRepository.findById(worker.getUserId()).orElseThrow(UserNotFoundException::new).getId();
        routineService.saveWorkRoutineMapping(workerUserId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workplaceId, Long workerId, Long workId) {
        VerifiedWorkContext context = getVerifiedWorkContext(userId, workplaceId, workerId, workId);

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
    public WorkSummaryResponse getSummarizedWork(Long userId, Long workplaceId, Long workerId, Long workId) {
        VerifiedWorkContext context = getVerifiedWorkContext(userId, workplaceId, workerId, workId);

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
    public WorkCalendarListResponse getAllSummarizedMyWorkForAllWorkplaces(Long userId, YearMonth baseYearMonth) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        // 1. 사용자의 모든 Worker 정보 조회 (쿼리 1)
        List<Worker> userWorkerList = workerRepository.findAllByUserId(userId);
        if (userWorkerList.isEmpty()) {
            return WorkCalendarListResponse.builder()
                    .workSummaryInfoList(Collections.emptyList())
                    .build();
        }

        // 2. ID 리스트 추출
        List<Long> workplaceIdList = userWorkerList.stream()
                .map(Worker::getWorkplaceId)
                .distinct()
                .toList();
        List<Long> workerIdList = userWorkerList.stream()
                .map(Worker::getId)
                .toList();

        // 3. Workplace 정보 한 번에 조회 (쿼리 2) 및 Map으로 변환
        Map<Long, Workplace> workplaceMap = workplaceRepository.findAllByIdIn(workplaceIdList).stream()
                .collect(Collectors.toMap(Workplace::getId, workplace -> workplace));

        // 4. Work 정보 한 번에 조회 (쿼리 3) 및 Map으로 변환 (workerId를 key로)
        List<Work> allWorks = workRepository.findAllByWorkerIdInAndDateRange(workerIdList, startDate, endDate);
        Map<Long, List<Work>> workMapByWorker = allWorks.stream()
                .collect(Collectors.groupingBy(Work::getWorkerId));

        // 5. DTO 조립 (추가 쿼리 없음)
        List<WorkSummaryResponse> userWorkSummaryList = new ArrayList<>();
        for (Worker userWorker : userWorkerList) {
            Workplace workplace = workplaceMap.get(userWorker.getWorkplaceId());
            // workplace가 null인 경우 방어 코드 (데이터 정합성이 깨졌을 경우)
            if (workplace == null) continue;

            verifyPermission(userId, userWorker.getUserId(), workplace.getOwnerId());

            WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(userWorker);
            WorkplaceSummaryResponse workplaceSummaryInfo = WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build();

            List<Work> workerWorks = workMapByWorker.getOrDefault(userWorker.getId(), Collections.emptyList());

            List<WorkSummaryResponse> workSummaryList = workerWorks.stream()
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
    public WorkCalendarListResponse getAllSummarizedWorkByWorkplace(User user, Long workplaceId, YearMonth baseYearMonth, Boolean isShared) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        verifyPermission(user.getId(), userWorker.getUserId(), workplace.getOwnerId());
        if (Boolean.TRUE.equals(isShared) && !workplace.isShared()) { throw new InvalidPermissionAccessException(); }

        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        List<WorkSummaryResponse> workSummaryInfoList = new ArrayList<>();
        if (Boolean.TRUE.equals(isShared)) {
            // 근무지의 모든 근무자 근무 반환
            List<Worker> workplaceWorkerList = workerRepository.findAllByWorkplaceId(workplaceId);
            for (Worker workplaceWorker : workplaceWorkerList) {
                List<Work> workerWorkList = workRepository.findAllByWorkerIdAndDateRange(workplaceWorker.getId(), startDate, endDate);

                WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(workplaceWorker);
                List<WorkSummaryResponse> workerWorkSummaryList = workerWorkList.stream()
                        .map(work -> {
                            long workMinutes = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();
                            boolean isEditable = checkEditable(user.getId(), workplaceWorker.getUserId(), workplace.getOwnerId());

                            return convertWorkToSummaryResponse(work, workerSummaryInfo, workplaceSummary, workMinutes, isEditable);
                        })
                        .toList();
                workSummaryInfoList.addAll(workerWorkSummaryList);
            }
        } else {
            // 사용자의 근무만 반환
            WorkerSummaryResponse userWorkerSummaryInfo = createWorkerSummary(userWorker);

            List<Work> userWorkList = workRepository.findAllByWorkerIdAndDateRange(userWorker.getId(), startDate, endDate);
            workSummaryInfoList = userWorkList.stream()
                    .map(userWork -> {
                        long workMinutes = Duration.between(userWork.getStartTime(), userWork.getEndTime()).toMinutes();
                        boolean isEditable = checkEditable(user.getId(), userWorker.getUserId(), workplace.getOwnerId());

                        return convertWorkToSummaryResponse(userWork, userWorkerSummaryInfo, workplaceSummary, workMinutes, isEditable);
                    })
                    .toList();
        }

        return WorkCalendarListResponse.builder()
                .workSummaryInfoList(workSummaryInfoList)
                .build();
    }

    @Transactional
    public void updateMyWork(Long userId, Long workplaceId, Long workId, WorkUpdateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        verifyPermission(userId, userWorker.getUserId(), workplaceId);
        if (!workRepository.existsByIdAndWorkerId(workId, userWorker.getId())) { throw new WorkNotFoundException(); }

        updateWorkHelper(userWorker, workId, request);

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), workId);
    }

    @Transactional
    public void updateWorkForWorkerId(Long requesterUserId, Long workplaceId, Long workerId, Long workId, WorkUpdateRequest request) {
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(requesterUserId, worker.getUserId(), workplaceOwnerId);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getId())) { throw new WorkNotFoundException(); }

        updateWorkHelper(worker, workId, request);

        Long workerUserId = userRepository.findById(worker.getUserId()).orElseThrow(UserNotFoundException::new).getId();
        routineService.saveWorkRoutineMapping(workerUserId, request.getRoutineIdList(), workId);
    }

    @Transactional
    public void deleteMyWork(Long userId, Long workplaceId, Long workId) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, userWorker.getId())) { throw new WorkNotFoundException(); }
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(userId, userWorker.getUserId(), workplaceOwnerId);

        routineService.deleteWorkRoutineMappingByWorkId(workId);

        Work work = workRepository.findByIdAndWorkerId(workId, userWorker.getId()).orElseThrow(WorkNotFoundException::new);
        deleteWorkHelper(userWorker, work);
    }

    @Transactional
    public void deleteWorkForWorker(Long requesterUserId, Long workplaceId, Long workerId, Long workId) {
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getId())) { throw new WorkNotFoundException(); }
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(requesterUserId, worker.getUserId(), workplaceOwnerId);

        routineService.deleteWorkRoutineMappingByWorkId(workId);

        Work work = workRepository.findByIdAndWorkerId(workId, worker.getId()).orElseThrow(WorkNotFoundException::new);
        deleteWorkHelper(worker, work);
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

    private VerifiedWorkContext getVerifiedWorkContext(Long userId, Long workplaceId, Long workerId, Long workId) {
        // 1. 요청자(현재 사용자)의 Worker 정보와 Workplace 정보 조회
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);

        // 2. 현재 사용자가 이 근무 기록에 접근할 권한이 있는지 검증
        verifyPermission(userId, userWorker.getUserId(), workplace.getOwnerId());

        // 3. 실제 요청된 근무(Work) 정보 조회
        Work work = workRepository.findByIdAndWorkerId(workId, workerId)
                .orElseThrow(WorkNotFoundException::new);

        long workMinutes = Duration.between(work.getStartTime(), work.getEndTime()).toMinutes();

        // 4. 근무를 수행한 근무자(Worker)와 사용자(User) 정보 조회
        Worker requestedWorker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);

        // 5. 근무자 요약 DTO 생성
        WorkerSummaryResponse workerSummaryInfo = createWorkerSummary(requestedWorker);

        // 6. 근무지 요약 DTO 생성
        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        // 7. 수정 가능 여부 계산
        boolean isEditable = checkEditable(userId, userWorker.getUserId(), workplace.getOwnerId());

        // 8. 모든 데이터를 컨테이너에 담아 반환
        return new VerifiedWorkContext(work, workMinutes, workerSummaryInfo, workplaceSummary, isEditable);
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

    private void verifyPermission(Long requesterUserId, Long workerUserId, Long workplaceOwnerId) {
        // 요청자가 해당 근무지의 근무자도 아니고 사장님도 아니면 예외 발생
        if (!workerUserId.equals(requesterUserId) && !workplaceOwnerId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
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
