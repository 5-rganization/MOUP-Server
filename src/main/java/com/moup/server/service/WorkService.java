package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Work;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkService {
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;

    private final RoutineService routineService;
    private final SalaryCalculationService salaryCalculationService;
    private final WorkplaceService workplaceService;

    @Transactional
    public WorkCreateResponse createWork(Long userId, Long workplaceId, WorkCreateRequest request) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(userId, worker.getUserId(), workplaceOwnerId);

        Work work = createWorkHelper(userId, worker, request);

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional
    public WorkCreateResponse createWorkForWorkerId(Long requesterId, Long workplaceId, Long workerId, WorkCreateRequest request) {
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(requesterId, worker.getUserId(), workplaceOwnerId);

        Work work = createWorkHelper(requesterId, worker, request);

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workplaceId, Long workId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        verifyPermission(userId, worker.getUserId(), workplace.getOwnerId());
        Work work = workRepository.findByIdAndWorkerId(workId, worker.getId()).orElseThrow(WorkNotFoundException::new);

        WorkplaceSummaryResponse workplaceSummary = WorkplaceSummaryResponse.builder()
                .workplaceId(workplace.getId())
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();

        List<RoutineSummaryResponse> routineSummaryList = routineService.getAllSummarizedRoutineByWorkRoutineMapping(userId, workId);

        List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(work.getRepeatDays());

        return WorkDetailResponse.builder()
                .workplaceSummary(workplaceSummary)
                .routineSummaryList(routineSummaryList)
                .workDate(work.getWorkDate())
                .startTime(work.getStartTime())
                .actualStartTime(work.getActualStartTime())
                .endTime(work.getEndTime())
                .actualEndTime(work.getActualEndTime())
                .restTimeMinutes(work.getRestTimeMinutes())
                .memo(work.getMemo())
                .repeatDays(repeatDays)
                .repeatEndDate(work.getRepeatEndDate())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkSummaryResponse getWorkSummary(Long userId, Long workplaceId, Long workId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(userId, worker.getUserId(), workplaceOwnerId);
        Work work = workRepository.findByIdAndWorkerId(workId, worker.getId()).orElseThrow(WorkNotFoundException::new);

        WorkplaceSummaryResponse workplaceSummary = workplaceService.getSummarizedWorkplace(userId, worker.getWorkplaceId());

        Duration workDuration = Duration.between(work.getStartTime(), work.getEndTime());
        long workMinutes = workDuration.toMinutes();

        List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(work.getRepeatDays());

        return WorkSummaryResponse.builder()
                .workplaceSummary(workplaceSummary)
                .workDate(work.getWorkDate())
                .startTime(work.getStartTime())
                .endTime(work.getEndTime())
                .workMinutes(workMinutes)
                .restTimeMinutes(work.getRestTimeMinutes())
                .repeatDays(repeatDays)
                .repeatEndDate(work.getRepeatEndDate())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkCalendarResponse getWorkCalendarSummary(Long userId, YearMonth baseYearMonth, Boolean isShared) {
        LocalDate startDate = baseYearMonth.minusMonths(6).atDay(1);
        LocalDate endDate = baseYearMonth.plusMonths(6).atEndOfMonth();

        ArrayList<WorkSummaryResponse> workSummaryList = new ArrayList<>();

        List<Worker> workerList = workerRepository.findAllByUserId(userId);
        for (Worker worker : workerList) {
            List<Work> workList = workRepository.findAllByWorkerIdAndDateRange(worker.getId(), startDate, endDate);
            for (Work work : workList) {
                WorkplaceSummaryResponse workplaceSummary = workplaceService.getSummarizedWorkplace(userId, worker.getWorkplaceId());
                if (workplaceSummary.getIsShared() != isShared) { continue; }

                Duration workDuration = Duration.between(work.getStartTime(), work.getEndTime());
                long workMinutes = workDuration.toMinutes();

                List<DayOfWeek> repeatDays = convertDayOfWeekStrToList(work.getRepeatDays());

                WorkSummaryResponse workSummaryResponse = WorkSummaryResponse.builder()
                        .workplaceSummary(workplaceSummary)
                        .workDate(work.getWorkDate())
                        .startTime(work.getStartTime())
                        .endTime(work.getEndTime())
                        .workMinutes(workMinutes)
                        .restTimeMinutes(work.getRestTimeMinutes())
                        .repeatDays(repeatDays)
                        .repeatEndDate(work.getRepeatEndDate())
                        .build();
                workSummaryList.add(workSummaryResponse);
            }
        }

        return WorkCalendarResponse.builder()
                .workSummaryList(workSummaryList)
                .build();
    }

    @Transactional
    public void updateWork(Long userId, Long workplaceId, Long workId, WorkUpdateRequest request) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        verifyPermission(userId, worker.getUserId(), workplaceId);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getUserId())) { throw new WorkNotFoundException(); }

        int hourlyRate = salaryRepository.findByWorkerId(worker.getId())
                .map(Salary::getHourlyRate)
                .orElse(0);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        Work work = request.toEntity(workId, worker.getUserId(), hourlyRate);
        workRepository.update(work);

        salaryCalculationService.recalculateWorkWeek(worker.getUserId(), work.getWorkDate());

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());
    }

    @Transactional
    public void deleteWork(Long userId, Long workplaceId, Long workId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getUserId())) { throw new WorkNotFoundException(); }
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        verifyPermission(userId, worker.getUserId(), workplaceOwnerId);

        routineService.deleteWorkRoutineMapping(workId);

        Work work = workRepository.findByIdAndWorkerId(workId, worker.getId()).orElseThrow(WorkNotFoundException::new);
        workRepository.delete(workId, worker.getUserId());

        salaryCalculationService.recalculateWorkWeek(worker.getUserId(), work.getWorkDate());
    }

    private Work createWorkHelper(Long userId, Worker worker, WorkCreateRequest request) {
        int hourlyRate = salaryRepository.findByWorkerId(worker.getId())
                .map(Salary::getHourlyRate)
                .orElse(0);

        verifyStartEndTime(request.getStartTime(), request.getEndTime());

        Work work = request.toEntity(worker.getId(), hourlyRate);
        workRepository.create(work);

        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());

        return work;
    }

    private void verifyPermission(Long userId, Long workerUserId, Long workplaceOwnerId) {
        // 요청자가 해당 근무지의 근무자도 아니고 사장님도 아니면 예외 발생
        if (!workerUserId.equals(userId) && !workplaceOwnerId.equals(userId)) {
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
}
