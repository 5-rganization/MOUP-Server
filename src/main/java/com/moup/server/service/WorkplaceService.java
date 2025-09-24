package com.moup.server.service;

import com.moup.server.exception.SalaryWorkerNotFoundException;
import com.moup.server.exception.WorkerWorkplaceNotFoundException;
import com.moup.server.exception.WorkplaceAlreadyExistsException;
import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import org.springframework.transaction.annotation.Transactional;
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
    protected Worker createWorkplaceAndWorker(Long userId, WorkplaceCreateRequest workplaceCreateRequest) {
        Workplace workplace = workplaceCreateRequest.toWorkplaceEntity(userId);
        if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, workplace.getWorkplaceName())) { throw new WorkplaceAlreadyExistsException(); }
        workplaceRepository.create(workplace);

        String workerBasedLabelColor = workplaceCreateRequest.getWorkerBasedLabelColor();
        String ownerBasedLabelColor = workplaceCreateRequest.getOwnerBasedLabelColor();

        Worker worker = Worker.builder()
                .userId(userId)
                .workplaceId(workplace.getId())
                .workerBasedLabelColor(workerBasedLabelColor == null ? "primary" : workerBasedLabelColor)
                .ownerBasedLabelColor(ownerBasedLabelColor == null ? "primary" : ownerBasedLabelColor)
                .isAccepted(true)
                .build();
        workerRepository.create(worker);

        return workerRepository.findByUserIdAndWorkplaceId(userId, worker.getWorkplaceId()).orElseThrow(WorkerWorkplaceNotFoundException::new);
    }

    @Transactional
    public WorkplaceCreateResponse createWorkerWorkplace(Long userId, WorkerWorkplaceCreateRequest workerWorkplaceCreateRequest) {
        Worker createdWorker = createWorkplaceAndWorker(userId, workerWorkplaceCreateRequest);

        Salary salaryToCreate = workerWorkplaceCreateRequest.toSalaryEntity(createdWorker.getId());
        salaryRepository.create(salaryToCreate);

        return WorkplaceCreateResponse.builder()
                .workplaceId(createdWorker.getWorkplaceId())
                .build();
    }

    @Transactional
    public WorkplaceCreateResponse createOwnerWorkplace(Long userId, OwnerWorkplaceCreateRequest ownerWorkplaceCreateRequest) {
        Worker createdWorker = createWorkplaceAndWorker(userId, ownerWorkplaceCreateRequest);

        return WorkplaceCreateResponse.builder()
                .workplaceId(createdWorker.getWorkplaceId())
                .build();
    }

    @Transactional(readOnly = true)
    public Workplace findByUserIdAndWorkplaceName(Long userId, String workplaceName) {
        return workplaceRepository.findByOwnerIdAndWorkplaceName(userId, workplaceName).orElseThrow(WorkplaceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<WorkplaceSummaryResponse> summarizeAllWorkplaceByUserId(Long userId) {
        List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

        List<WorkplaceSummaryResponse> workplaceSummaryResponses = new ArrayList<>();
        for (Worker worker : userAllWorkers) {
            Workplace workplace = workplaceRepository.findById(worker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new);
            workplaceSummaryResponses.add(WorkplaceSummaryResponse.builder()
                    .workplaceId(workplace.getId())
                    .workplaceName(workplace.getWorkplaceName())
                    .isShared(workplace.isShared())
                    .build());
        }
        return workplaceSummaryResponses;
    }

    @Transactional
    protected void updateWorkplace(Long userId, WorkplaceUpdateRequest workplaceUpdateRequest) {
        Workplace newWorkplace = workplaceUpdateRequest.toWorkplaceEntity(userId);
        Workplace oldWorkplace = workplaceRepository.findById(newWorkplace.getId()).orElseThrow(WorkplaceNotFoundException::new);
        if (!newWorkplace.getWorkplaceName().equals(oldWorkplace.getWorkplaceName())
                && workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, newWorkplace.getWorkplaceName())) { throw new WorkplaceAlreadyExistsException(); }
        workplaceRepository.update(newWorkplace);
    }

    @Transactional
    public void updateWorkerWorkplace(Long userId, WorkerWorkplaceUpdateRequest workerWorkplaceUpdateRequest) {
        updateWorkplace(userId, workerWorkplaceUpdateRequest);

        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workerWorkplaceUpdateRequest.getWorkplaceId()).orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        workerRepository.updateWorkerBasedLabelColor(workerId, userId, workerWorkplaceUpdateRequest.getWorkplaceId(), workerWorkplaceUpdateRequest.getWorkerBasedLabelColor());

        Long salaryId = salaryRepository.findByIdAndWorkerId(userId, workerId).orElseThrow(SalaryWorkerNotFoundException::new).getId();
        Salary newSalary = workerWorkplaceUpdateRequest.toSalaryEntity(salaryId, workerId);
        salaryRepository.update(newSalary);
    }

    @Transactional
    public void updateOwnerWorkplace(Long userId, OwnerWorkplaceUpdateRequest ownerWorkplaceUpdateRequest) {
        updateWorkplace(userId, ownerWorkplaceUpdateRequest);
        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, ownerWorkplaceUpdateRequest.getWorkplaceId()).orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        workerRepository.updateOwnerBasedLabelColor(workerId, userId, ownerWorkplaceUpdateRequest.getWorkplaceId(), ownerWorkplaceUpdateRequest.getOwnerBasedLabelColor());
    }

    @Transactional
    public void deleteWorkplace(Long userId, Long workplaceId) {
        if (workplaceRepository.existsByIdAndOwnerId(workplaceId, userId)) {
            workplaceRepository.deleteByWorkplaceIdAndOwnerId(workplaceId, userId);
        } else {
            throw new WorkplaceNotFoundException();
        }
    }
}
