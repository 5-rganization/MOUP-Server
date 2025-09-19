package com.moup.server.service;

import com.moup.server.exception.SalaryNotFoundException;
import com.moup.server.exception.WorkerNotFoundException;
import com.moup.server.exception.WorkplaceAlreadyExistsException;
import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkplaceService {
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;

    @Transactional
    protected Worker createWorkplace(Long userId, WorkplaceCreateRequest workplaceCreateRequest) {
        Workplace workplace = workplaceCreateRequest.toWorkplaceEntity(userId);
        if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, workplace.getWorkplaceName())) {
            throw new WorkplaceAlreadyExistsException();
        }

        workplaceRepository.create(workplace);
        Workplace createdWorkplace = workplaceRepository.findByOwnerIdAndWorkplaceName(userId, workplaceCreateRequest.getWorkplaceName()).orElseThrow(WorkplaceNotFoundException::new);

        Worker worker = Worker.builder().userId(userId).workplaceId(createdWorkplace.getId()).labelColor(createdWorkplace.getLabelColor()).isAccepted(true).build();
        workerRepository.create(worker);
        return workerRepository.findByUserIdAndWorkplaceId(userId, worker.getWorkplaceId()).orElseThrow(WorkerNotFoundException::new);
    }

    @Transactional
    public WorkplaceCreateResponse createWorkerWorkplace(Long userId, WorkerWorkplaceCreateRequest workerWorkplaceCreateRequest) {
        Worker createdWorker = createWorkplace(userId, workerWorkplaceCreateRequest);

        Salary salary = workerWorkplaceCreateRequest.toSalaryEntity(createdWorker.getId());
        salaryRepository.create(salary);

        return WorkplaceCreateResponse.builder().workplaceId(createdWorker.getWorkplaceId()).workerId(createdWorker.getId()).build();
    }

    @Transactional
    public WorkplaceCreateResponse createOwnerWorkplace(Long userId, OwnerWorkplaceCreateRequest ownerWorkplaceCreateRequest) {
        Worker createdWorker = createWorkplace(userId, ownerWorkplaceCreateRequest);

        return WorkplaceCreateResponse.builder().workplaceId(createdWorker.getWorkplaceId()).workerId(createdWorker.getId()).build();
    }

    public WorkplaceDuplicateCheckResponse checkDuplicateWorkplace(Long userId, String workplaceName) {
        boolean isDuplicated = workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, workplaceName);
        return WorkplaceDuplicateCheckResponse.builder().isDuplicated(isDuplicated).build();
    }

    public Workplace findByUserIdAndWorkplaceName(Long userId, String workplaceName) {
        return workplaceRepository.findByOwnerIdAndWorkplaceName(userId, workplaceName).orElseThrow(WorkplaceNotFoundException::new);
    }

    public List<WorkplaceSummaryResponse> summarizeAllWorkplaceByUserId(Long userId) {
        List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

        if (userAllWorkers.isEmpty()) {
            throw new WorkerNotFoundException();
        }

        List<WorkplaceSummaryResponse> workplaceSummaryResponses = new ArrayList<>();
        for (Worker worker : userAllWorkers) {
            Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new);
            workplaceSummaryResponses.add(WorkplaceSummaryResponse.builder().workplaceId(workplace.getId()).workplaceName(workplace.getWorkplaceName()).isShared(workplace.isShared()).build());
        }
        return workplaceSummaryResponses;
    }

    @Transactional
    protected Workplace updateWorkplace(Long userId, WorkplaceUpdateRequest workplaceUpdateRequest) {
        Workplace workplace = workplaceUpdateRequest.toWorkplaceEntity(userId);
        if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, workplace.getWorkplaceName())) {
            throw new WorkplaceAlreadyExistsException();
        }

        workplaceRepository.update(workplace);

        return workplaceRepository.findByOwnerIdAndWorkplaceName(userId, workplace.getWorkplaceName()).orElseThrow(WorkplaceNotFoundException::new);
    }

    @Transactional
    public WorkerWorkplaceUpdateResponse updateWorkerWorkplace(Long userId, WorkerWorkplaceUpdateRequest workerWorkplaceUpdateRequest) {
        Workplace updatedWorkplace = updateWorkplace(userId, workerWorkplaceUpdateRequest);

        Salary salary = workerWorkplaceUpdateRequest.toSalaryEntity(updatedWorkplace.getId());
        salaryRepository.update(salary);
        Salary updatedSalary = salaryRepository.findByWorkerId(salary.getId()).orElseThrow(SalaryNotFoundException::new);

        return WorkerWorkplaceUpdateResponse.builder().workplaceId(updatedWorkplace.getId()).workerId(updatedSalary.getId()).build();
    }

    @Transactional
    public OwnerWorkplaceUpdateResponse updateOwnerWorkplace(Long userId, OwnerWorkplaceUpdateRequest ownerWorkplaceUpdateRequest) {
        Workplace updatedWorkplace = updateWorkplace(userId, ownerWorkplaceUpdateRequest);

        return OwnerWorkplaceUpdateResponse.builder().workplaceId(updatedWorkplace.getId()).build();
    }

    public WorkplaceDeleteResponse deleteWorkplace(Long userId, Long workplaceId) {
        workplaceRepository.deleteByWorkplaceIdAndOwnerId(workplaceId, userId);

        return WorkplaceDeleteResponse.builder().workplaceId(workplaceId).build();
    }
}
