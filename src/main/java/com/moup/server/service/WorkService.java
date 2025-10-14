package com.moup.server.service;

import com.moup.server.exception.*;
import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
import com.moup.server.model.dto.WorkDetailResponse;
import com.moup.server.model.dto.WorkUpdateRequest;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Work;
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
        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        checkPermission(userId, workerId, workplaceId);
        Salary salary = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new);

        // 1. Work 엔티티 생성 및 기본 정보 저장
        Work work = request.toEntity(workerId, salary.getHourlyRate());
        workRepository.create(work); // DB에 먼저 저장되어야 ID가 생성됨

        // 2. 해당 근무일이 포함된 주의 모든 근무 기록을 다시 계산 (주휴수당 때문)
        salaryCalculationService.recalculateWorkWeek(workerId, work.getWorkDate());

        work = request.toEntity(workerId, salary.getHourlyRate());
        workRepository.create(work);

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }


    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workId) {
        // TODO: 상세 조회 로직 구현
        return null;
    }

    @Transactional
    public void updateWork(Long userId, Long workplaceId, Long workId, WorkUpdateRequest request) {
        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        if (!workRepository.existsByIdAndWorkerId(workId, workerId)) { throw new WorkNotFoundException(); }
        checkPermission(userId, workerId, workplaceId);
        Salary salary = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new);

        Work work = request.toEntity(workId, workerId, salary.getHourlyRate());
        workRepository.update(work);

        salaryCalculationService.recalculateWorkWeek(workerId, work.getWorkDate());

        routineService.saveWorkRoutineMapping(userId, request.getRoutineIdList(), work.getId());
    }

    @Transactional
    public void deleteWork(Long userId, Long workplaceId, Long workId) {
        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        if (!workRepository.existsByIdAndWorkerId(workId, workerId)) { throw new WorkNotFoundException(); }
        checkPermission(userId, workerId, workplaceId);

        routineService.deleteWorkRoutineMapping(workId);

        Work work = workRepository.findById(workId).orElseThrow(WorkNotFoundException::new);
        workRepository.delete(workId, workerId);

        salaryCalculationService.recalculateWorkWeek(workerId, work.getWorkDate());
    }

    private void checkPermission(Long userId, Long workerId, Long workplaceId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        if (!workerId.equals(userId) || !workplaceOwnerId.equals(userId)) { throw new InvalidPermissionAccessException(); }
    }
}
