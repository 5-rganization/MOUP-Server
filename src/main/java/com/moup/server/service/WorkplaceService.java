package com.moup.server.service;

import com.moup.server.common.Role;
import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkplaceService {
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;
    private final InviteCodeService inviteCodeService;

    @Transactional
    protected Worker createWorkplaceAndWorkerHelper(Long userId, BaseWorkplaceCreateRequest request) {
        if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, request.getWorkplaceName())) { throw new WorkplaceNameAlreadyUsedException(); }

        Workplace workplaceToCreate = request.toWorkplaceEntity(userId);
        workplaceRepository.create(workplaceToCreate);

        Worker workerToCreate = request.toWorkerEntity(userId, workplaceToCreate.getId());
        workerRepository.create(workerToCreate);

        return workerToCreate;
    }

    @Transactional
    public WorkplaceCreateResponse createWorkplace(User user, BaseWorkplaceCreateRequest request) {
        if (user.getRole() == Role.ROLE_OWNER && request instanceof OwnerWorkplaceCreateRequest ownerWorkplaceCreateRequest) {
            Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), ownerWorkplaceCreateRequest);
            return WorkplaceCreateResponse.builder()
                    .workplaceId(createdWorker.getWorkplaceId())
                    .build();
        } else if (user.getRole() == Role.ROLE_WORKER && request instanceof WorkerWorkplaceCreateRequest workerWorkplaceCreateRequest) {
            Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), workerWorkplaceCreateRequest);

            Salary salaryToCreate = workerWorkplaceCreateRequest.toSalaryEntity(createdWorker.getId());
            salaryRepository.create(salaryToCreate);

            return WorkplaceCreateResponse.builder()
                    .workplaceId(createdWorker.getWorkplaceId())
                    .build();
        } else {
            throw new InvalidPermissionAccessException();
        }
    }

    @Transactional(readOnly = true)
    public List<WorkplaceSummaryResponse> getAllSummarizedWorkplace(Long userId) {
        List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

        return userAllWorkers.stream()
                .map(worker -> workplaceRepository.findById(worker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new))
                .map(workplace -> WorkplaceSummaryResponse.builder()
                        .workplaceId(workplace.getId())
                        .workplaceName(workplace.getWorkplaceName())
                        .isShared(workplace.isShared())
                        .build())
                .sorted(Comparator.comparing(WorkplaceSummaryResponse::getWorkplaceName))
                .toList();
    }

    @Transactional
    protected Long updateWorkplaceAndWorkerHelper(Long userId, Long workplaceId, BaseWorkplaceUpdateRequest request) {
        Workplace oldWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!oldWorkplace.getWorkplaceName().equals(request.getWorkplaceName())
                && workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, request.getWorkplaceName())) { throw new WorkplaceNameAlreadyUsedException(); }

        Workplace newWorkplace = request.toWorkplaceEntity(workplaceId, userId);
        workplaceRepository.update(newWorkplace);

        return workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
    }

    @Transactional
    public void updateWorkplace(User user, Long workplaceId, BaseWorkplaceUpdateRequest request) {
        if (user.getRole() == Role.ROLE_OWNER && request instanceof OwnerWorkplaceUpdateRequest ownerRequest) {
            Long workerId = updateWorkplaceAndWorkerHelper(user.getId(), workplaceId, ownerRequest);
            workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId, ownerRequest.getOwnerBasedLabelColor());
        } else if (user.getRole() == Role.ROLE_WORKER && request instanceof WorkerWorkplaceUpdateRequest workerRequest) {
            Long workerId = updateWorkplaceAndWorkerHelper(user.getId(), workplaceId, workerRequest);
            workerRepository.updateWorkerBasedLabelColor(workerId, user.getId(), workplaceId, workerRequest.getWorkerBasedLabelColor());

            Long salaryId = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new).getId();
            Salary newSalary = workerRequest.toSalaryEntity(salaryId, workerId);
            salaryRepository.update(newSalary);
        } else {
            throw new InvalidPermissionAccessException();
        }
    }

    @Transactional
    public void deleteWorkplace(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (workplace.getOwnerId().equals(userId)) {
            workplaceRepository.deleteByIdAndOwnerId(workplaceId, userId);
        } else {
            Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new);
            workerRepository.delete(worker.getId(), userId, workplaceId);
        }
    }

    @Transactional
    public InviteCodeGenerateResponse generateInviteCode(User user, Long workplaceId, InviteCodeGenerateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!workplace.getOwnerId().equals(user.getId()) || user.getRole() != Role.ROLE_OWNER) { throw new InvalidPermissionAccessException(); }

        boolean returnAlreadyExists = !request.getForceGenerate() && inviteCodeService.existsByWorkplaceId(workplaceId);
        String inviteCode = inviteCodeService.generateInviteCode(workplaceId, request.getForceGenerate());

        return InviteCodeGenerateResponse.builder()
                .inviteCode(inviteCode)
                .returnAlreadyExists(returnAlreadyExists)
                .build();
    }

    @Transactional(readOnly = true)
    public InviteCodeInquiryResponse inquireInviteCode(User user, String inviteCode) {
        if (user.getRole() != Role.ROLE_WORKER) { throw new InvalidPermissionAccessException(); }

        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(inviteCode);
        if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }

        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);

        return InviteCodeInquiryResponse.builder()
                .workplaceId(workplaceId)
                .categoryName(workplace.getCategoryName())
                .address(workplace.getAddress())
                .latitude(workplace.getLatitude())
                .longitude(workplace.getLongitude())
                .build();
    }

    @Transactional
    public WorkplaceJoinResponse joinWorkplace(User user, String inviteCode, WorkplaceJoinRequest request) {
        if (user.getRole() != Role.ROLE_WORKER) { throw new InvalidPermissionAccessException(); }

        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(inviteCode);
        if (!workplaceRepository.existsById(workplaceId)) { throw new WorkplaceNotFoundException(); }
        if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }

        Worker worker = Worker.builder()
                .userId(user.getId())
                .workplaceId(workplaceId)
                .workerBasedLabelColor(request.getWorkerBasedLabelColor())
                .isAccepted(false)
                .build();
        workerRepository.create(worker);

        WorkerSalaryCreateRequest salaryInfo = request.getSalaryInfo();
        Salary salary = Salary.builder()
                .workerId(worker.getId())
                .salaryType(salaryInfo.getSalaryType())
                .salaryCalculation(salaryInfo.getSalaryCalculation())
                .hourlyRate(salaryInfo.getHourlyRate())
                .fixedRate(salaryInfo.getFixedRate())
                .salaryDate(salaryInfo.getSalaryDate())
                .salaryDay(salaryInfo.getSalaryDay())
                .hasNationalPension(salaryInfo.getHasNationalPension())
                .hasHealthInsurance(salaryInfo.getHasHealthInsurance())
                .hasEmploymentInsurance(salaryInfo.getHasEmploymentInsurance())
                .hasIndustrialAccident(salaryInfo.getHasIndustrialAccident())
                .hasIncomeTax(salaryInfo.getHasIncomeTax())
                .hasNightAllowance(salaryInfo.getHasNightAllowance())
                .build();
        salaryRepository.create(salary);

        return WorkplaceJoinResponse.builder()
                .workplaceId(workplaceId)
                .workerId(worker.getId())
                .build();
    }
}
