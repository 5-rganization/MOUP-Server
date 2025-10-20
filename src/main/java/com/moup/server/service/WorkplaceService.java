package com.moup.server.service;

import com.moup.server.common.Role;
import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.*;
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
    private final WorkRepository workRepository;
    private final MonthlySalaryRepository monthlySalaryRepository;

    private final InviteCodeService inviteCodeService;

    // ========== 근무지 메서드 ==========

    @Transactional
    public WorkplaceCreateResponse createWorkplace(User user, BaseWorkplaceCreateRequest request) {
        if (user.getRole() == Role.ROLE_OWNER && request instanceof OwnerWorkplaceCreateRequest ownerWorkplaceCreateRequest) {
            Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), ownerWorkplaceCreateRequest);
            return WorkplaceCreateResponse.builder()
                    .workplaceId(createdWorker.getWorkplaceId())
                    .build();
        } else if (user.getRole() == Role.ROLE_WORKER && request instanceof WorkerWorkplaceCreateRequest workerWorkplaceCreateRequest) {
            Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), workerWorkplaceCreateRequest);

            Salary salaryToCreate = workerWorkplaceCreateRequest.getSalaryCreateRequest().toEntity(createdWorker.getId());
            salaryRepository.create(salaryToCreate);

            return WorkplaceCreateResponse.builder()
                    .workplaceId(createdWorker.getWorkplaceId())
                    .build();
        } else {
            throw new InvalidPermissionAccessException();
        }
    }

    @Transactional(readOnly = true)
    public BaseWorkplaceDetailResponse getWorkplaceDetail(User user, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerNotFoundException::new);

        if (user.getRole() == Role.ROLE_WORKER) {
            Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElseThrow(SalaryWorkerNotFoundException::new);
            SalaryDetailResponse salaryInfo = SalaryDetailResponse.builder()
                    .salaryType(salary.getSalaryType())
                    .salaryCalculation(salary.getSalaryCalculation())
                    .hourlyRate(salary.getHourlyRate())
                    .salaryDate(salary.getSalaryDate())
                    .hasNationalPension(salary.getHasNationalPension())
                    .hasHealthInsurance(salary.getHasHealthInsurance())
                    .hasEmploymentInsurance(salary.getHasEmploymentInsurance())
                    .hasIndustrialAccident(salary.getHasIndustrialAccident())
                    .hasIncomeTax(salary.getHasIncomeTax())
                    .hasNightAllowance(salary.getHasNightAllowance())
                    .build();

            return WorkerWorkplaceDetailResponse.builder()
                    .workplaceId(workplaceId)
                    .workplaceName(workplace.getWorkplaceName())
                    .categoryName(workplace.getCategoryName())
                    .address(workplace.getAddress())
                    .latitude(workplace.getLatitude())
                    .longitude(workplace.getLongitude())
                    .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                    .salaryDetailInfo(salaryInfo)
                    .build();
        } else if (user.getRole() == Role.ROLE_OWNER) {
            return OwnerWorkplaceDetailResponse.builder()
                    .workplaceId(workplaceId)
                    .workplaceName(workplace.getWorkplaceName())
                    .categoryName(workplace.getCategoryName())
                    .address(workplace.getAddress())
                    .latitude(workplace.getLatitude())
                    .longitude(workplace.getLongitude())
                    .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                    .build();
        } else {
            throw new InvalidPermissionAccessException();
        }
    }

    @Transactional(readOnly = true)
    public WorkplaceSummaryResponse getWorkplace(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!workerRepository.existsByUserIdAndWorkplaceId(userId, workplaceId)) { throw new WorkerNotFoundException(); }

        return WorkplaceSummaryResponse.builder()
                .workplaceId(workplaceId)
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkplaceSummaryResponse> getAllWorkplace(Long userId, Boolean isShared) {
        List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

        return userAllWorkers.stream()
                .map(worker -> workplaceRepository.findById(worker.getWorkplaceId()).orElseThrow(WorkplaceNotFoundException::new))
                .filter(workplace -> isShared == null || workplace.isShared() == isShared)
                .map(workplace -> WorkplaceSummaryResponse.builder()
                        .workplaceId(workplace.getId())
                        .workplaceName(workplace.getWorkplaceName())
                        .isShared(workplace.isShared())
                        .build())
                .sorted(Comparator.comparing(WorkplaceSummaryResponse::getWorkplaceName))
                .toList();
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
            Salary newSalary = workerRequest.getSalaryUpdateRequest().toEntity(salaryId, workerId);
            salaryRepository.update(newSalary);
        } else {
            throw new InvalidPermissionAccessException();
        }
    }

    @Transactional
    public void deleteWorkplace(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (workplace.getOwnerId().equals(userId)) {
            workplaceRepository.delete(workplaceId, userId);
        } else {
            Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new).getId();
            workerRepository.delete(workerId, userId, workplaceId);
        }
    }

    private Worker createWorkplaceAndWorkerHelper(Long userId, BaseWorkplaceCreateRequest request) {
        if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, request.getWorkplaceName())) { throw new WorkplaceNameAlreadyUsedException(); }

        Workplace workplaceToCreate = request.toWorkplaceEntity(userId);
        workplaceRepository.create(workplaceToCreate);

        Worker workerToCreate = request.toWorkerEntity(userId, workplaceToCreate.getId());
        workerRepository.create(workerToCreate);

        return workerToCreate;
    }

    private Long updateWorkplaceAndWorkerHelper(Long userId, Long workplaceId, BaseWorkplaceUpdateRequest request) {
        Workplace oldWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!oldWorkplace.getWorkplaceName().equals(request.getWorkplaceName())
                && workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, request.getWorkplaceName())) { throw new WorkplaceNameAlreadyUsedException(); }

        Workplace newWorkplace = request.toWorkplaceEntity(workplaceId, userId);
        workplaceRepository.update(newWorkplace);

        return workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new).getId();
    }

    // ========== 초대 코드 메서드 ==========

    @Transactional
    public InviteCodeGenerateResponse generateInviteCode(User user, Long workplaceId, InviteCodeGenerateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!workplace.getOwnerId().equals(user.getId()) || user.getRole() != Role.ROLE_OWNER) { throw new InvalidPermissionAccessException(); }

        boolean returnAlreadyExists = !request.isForceGenerate() && inviteCodeService.existsByWorkplaceId(workplaceId);
        String inviteCode = inviteCodeService.generateInviteCode(workplaceId, request.isForceGenerate());

        return InviteCodeGenerateResponse.builder()
                .inviteCode(inviteCode)
                .returnAlreadyExists(returnAlreadyExists)
                .build();
    }

    @Transactional(readOnly = true)
    public InviteCodeInquiryResponse inquireInviteCode(User user, String inviteCode) {
        if (user.getRole() != Role.ROLE_WORKER) { throw new InvalidPermissionAccessException(); }

        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(inviteCode.toUpperCase());
        if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }

        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);

        return InviteCodeInquiryResponse.builder()
                .workplaceId(workplaceId)
                .workplaceName(workplace.getWorkplaceName())
                .categoryName(workplace.getCategoryName())
                .address(workplace.getAddress())
                .latitude(workplace.getLatitude())
                .longitude(workplace.getLongitude())
                .build();
    }

    @Transactional
    public WorkplaceJoinResponse joinWorkplace(User user, WorkplaceJoinRequest request) {
        if (user.getRole() != Role.ROLE_WORKER) { throw new InvalidPermissionAccessException(); }

        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(request.getInviteCode().toUpperCase());
        if (!workplaceRepository.existsById(workplaceId)) { throw new WorkplaceNotFoundException(); }
        if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }

        Worker worker = Worker.builder()
                .userId(user.getId())
                .workplaceId(workplaceId)
                .workerBasedLabelColor(request.getWorkerBasedLabelColor())
                .isAccepted(false)
                .build();
        workerRepository.create(worker);

        SalaryCreateRequest salaryInfo = request.getSalaryCreateRequest();
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
