package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
import com.moup.server.model.dto.WorkDetailResponse;
import com.moup.server.model.dto.WorkUpdateRequest;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Work;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;

    private final RoutineService routineService;
    private final SalaryCalculationService salaryCalculationService;

    @Transactional
    public WorkCreateResponse createWork(Long userId, Long workplaceId, WorkCreateRequest request) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        checkPermission(userId, worker.getUserId(), workplaceId);
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);

        // 1. Work 엔티티 생성 및 기본 정보 저장
        Work work = request.toEntity(worker.getId(), salary.getHourlyRate());
        workRepository.create(work); // DB에 먼저 저장되어야 ID가 생성됨

        // 2. 해당 근무일이 포함된 주의 모든 근무 기록을 다시 계산 (주휴수당 때문)
        salaryCalculationService.recalculateWorkWeek(worker.getId(), work.getWorkDate());

        work = request.toEntity(worker.getId(), salary.getHourlyRate());
        workRepository.create(work);

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }


    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workplaceId, Long workId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getId())) { throw new WorkNotFoundException(); }
        checkPermission(userId, worker.getUserId(), workplaceId);
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);
        return null;
    }

//    @Transactional(readOnly = true)
//    public WorkSummaryResponse getWorkSummary() {
//
//    }

    @Transactional
    public void updateWork(Long userId, Long workplaceId, Long workId, WorkUpdateRequest request) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getUserId())) { throw new WorkNotFoundException(); }
        checkPermission(userId, worker.getUserId(), workplaceId);
        Salary salary = salaryRepository.findByWorkerId(worker.getUserId()).orElseThrow(SalaryWorkerNotFoundException::new);

        Work work = request.toEntity(workId, worker.getUserId(), salary.getHourlyRate());
        workRepository.update(work);

        salaryCalculationService.recalculateWorkWeek(worker.getUserId(), work.getWorkDate());

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());
    }

    @Transactional
    public void deleteWork(Long userId, Long workplaceId, Long workId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new);
        if (!workRepository.existsByIdAndWorkerId(workId, worker.getUserId())) { throw new WorkNotFoundException(); }
        checkPermission(userId, worker.getUserId(), workplaceId);

        routineService.deleteWorkRoutineMapping(workId);

        Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
        workRepository.delete(workId, worker.getUserId());

        salaryCalculationService.recalculateWorkWeek(worker.getUserId(), work.getWorkDate());
    }

    private void checkPermission(Long userId, Long workerUserId, Long workplaceId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        if (!workerUserId.equals(userId) || !workplaceOwnerId.equals(userId)) {
            throw new InvalidPermissionAccessException();
        }
    }
}
