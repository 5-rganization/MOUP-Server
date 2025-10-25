package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.server.common.AlarmContent;
import com.moup.server.common.AlarmTitle;
import com.moup.server.common.Role;
import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.*;
import com.moup.server.util.PermissionVerifyUtil;
import org.springframework.beans.factory.annotation.Value;
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
  private final PermissionVerifyUtil permissionVerifyUtil;
  private final FCMService fcmService;
  @Value("${workplace.creation.limit}")
  private int workplaceCreationLimit;

  // ========== 근무지 메서드 ==========

  @Transactional
  public WorkplaceCreateResponse createWorkplace(User user, BaseWorkplaceCreateRequest request) {
    return switch (user.getRole()) {
      case ROLE_OWNER -> {
        // switch는 Role로만 분기하므로, request 타입 체크는 case 내부에서 수행
        if (!(request instanceof OwnerWorkplaceCreateRequest ownerRequest)) {
          throw new InvalidPermissionAccessException();
        }
        Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), ownerRequest);
        yield WorkplaceCreateResponse.builder()
            .workplaceId(createdWorker.getWorkplaceId())
            .build();
      }
      case ROLE_WORKER -> {
        if (!(request instanceof WorkerWorkplaceCreateRequest workerRequest)) {
          throw new InvalidPermissionAccessException();
        }
        Worker createdWorker = createWorkplaceAndWorkerHelper(user.getId(), workerRequest);

        Salary salaryToCreate = workerRequest.getSalaryCreateRequest()
            .toEntity(createdWorker.getId());
        salaryRepository.create(salaryToCreate);

        yield WorkplaceCreateResponse.builder()
            .workplaceId(createdWorker.getWorkplaceId())
            .build();
      }
      // ADMIN 등 다른 역할은 허용하지 않음
      case ROLE_ADMIN -> throw new InvalidPermissionAccessException();
    };
  }

  @Transactional(readOnly = true)
  public BaseWorkplaceDetailResponse getWorkplaceDetail(User user, Long workplaceId) {
    Workplace workplace = workplaceRepository.findById(workplaceId)
        .orElseThrow(WorkplaceNotFoundException::new);
    Worker worker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId)
        .orElseThrow(WorkerNotFoundException::new);

    return switch (user.getRole()) {
      case ROLE_WORKER -> {
        Salary salary = salaryRepository.findByWorkerId(worker.getId())
            .orElseThrow(SalaryWorkerNotFoundException::new);
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

        yield WorkerWorkplaceDetailResponse.builder()
            .workplaceId(workplaceId)
            .workplaceName(workplace.getWorkplaceName())
            .categoryName(workplace.getCategoryName())
            .address(workplace.getAddress())
            .latitude(workplace.getLatitude())
            .longitude(workplace.getLongitude())
            .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
            .salaryDetailInfo(salaryInfo)
            .build();
      }
      case ROLE_OWNER -> OwnerWorkplaceDetailResponse.builder()
          .workplaceId(workplaceId)
          .workplaceName(workplace.getWorkplaceName())
          .categoryName(workplace.getCategoryName())
          .address(workplace.getAddress())
          .latitude(workplace.getLatitude())
          .longitude(workplace.getLongitude())
          .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
          .build();
      case ROLE_ADMIN -> throw new InvalidPermissionAccessException();
    };
  }

  @Transactional(readOnly = true)
  public WorkplaceSummaryResponse getWorkplace(Long userId, Long workplaceId) {
    Workplace workplace = workplaceRepository.findById(workplaceId)
        .orElseThrow(WorkplaceNotFoundException::new);
    if (!workerRepository.existsByUserIdAndWorkplaceId(userId, workplaceId)) {
      throw new WorkerNotFoundException();
    }

    return WorkplaceSummaryResponse.builder()
        .workplaceId(workplaceId)
        .workplaceName(workplace.getWorkplaceName())
        .isShared(workplace.isShared())
        .build();
  }

  @Transactional(readOnly = true)
  public List<WorkplaceSummaryResponse> getAllWorkplace(Long userId, boolean isShared) {
    List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

    return userAllWorkers.stream()
        .map(worker -> workplaceRepository.findById(worker.getWorkplaceId())
            .orElseThrow(WorkplaceNotFoundException::new))
        .filter(workplace -> workplace.isShared() == isShared)
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
    switch (user.getRole()) {
      case ROLE_OWNER -> {
        if (!(request instanceof OwnerWorkplaceUpdateRequest ownerRequest)) {
          throw new InvalidPermissionAccessException();
        }
        Long workerId = updateWorkplaceAndWorkerHelper(user.getId(), workplaceId, ownerRequest);
        workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId,
            ownerRequest.getOwnerBasedLabelColor());
      }
      case ROLE_WORKER -> {
        if (!(request instanceof WorkerWorkplaceUpdateRequest workerRequest)) {
          throw new InvalidPermissionAccessException();
        }
        Long workerId = updateWorkplaceAndWorkerHelper(user.getId(), workplaceId, workerRequest);
        workerRepository.updateWorkerBasedLabelColor(workerId, user.getId(), workplaceId,
            workerRequest.getWorkerBasedLabelColor());

        Long salaryId = salaryRepository.findByWorkerId(workerId)
            .orElseThrow(SalaryWorkerNotFoundException::new).getId();
        Salary newSalary = workerRequest.getSalaryUpdateRequest().toEntity(salaryId, workerId);
        salaryRepository.update(newSalary);
      }
      case ROLE_ADMIN -> throw new InvalidPermissionAccessException();
    }
  }

  @Transactional
  public void deleteWorkplace(Long userId, Long workplaceId) {
    Workplace workplace = workplaceRepository.findById(workplaceId)
        .orElseThrow(WorkplaceNotFoundException::new);
    if (workplace.getOwnerId().equals(userId)) {
      // 근무지(매장)을 만든 사용자가 삭제하는 경우
      workplaceRepository.delete(workplaceId, userId);
    } else {
      // 근무자가 근무지에서 탈퇴하는 경우
      Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
          .orElseThrow(WorkerNotFoundException::new).getId();
      workerRepository.delete(workerId, userId, workplaceId);
    }
  }

  private Worker createWorkplaceAndWorkerHelper(Long userId, BaseWorkplaceCreateRequest request) {
    if (workplaceRepository.existsByOwnerIdAndWorkplaceName(userId, request.getWorkplaceName())) {
      throw new WorkplaceNameAlreadyUsedException();
    }

    // TODO: JUnit으로 단위 테스트하기
    if (workplaceRepository.getOwnedWorkplaceCountByUserId(userId) >= workplaceCreationLimit) {
      throw new WorkplaceLimitExceededException(ErrorCode.WORKPLACE_LIMIT_EXCEEDED);
    }

    Workplace workplaceToCreate = request.toWorkplaceEntity(userId);
    workplaceRepository.create(workplaceToCreate);

    Worker workerToCreate = request.toWorkerEntity(userId, workplaceToCreate.getId());
    workerRepository.create(workerToCreate);

    return workerToCreate;
  }

  private Long updateWorkplaceAndWorkerHelper(Long userId, Long workplaceId,
      BaseWorkplaceUpdateRequest request) {
    Workplace oldWorkplace = workplaceRepository.findById(workplaceId)
        .orElseThrow(WorkplaceNotFoundException::new);
    if (!oldWorkplace.getWorkplaceName().equals(request.getWorkplaceName())
        && workplaceRepository.existsByOwnerIdAndWorkplaceName(userId,
        request.getWorkplaceName())) {
      throw new WorkplaceNameAlreadyUsedException();
    }

    Workplace newWorkplace = request.toWorkplaceEntity(workplaceId, userId);
    workplaceRepository.update(newWorkplace);

    return workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
        .orElseThrow(WorkerNotFoundException::new).getId();
  }

  // ========== 초대 코드 메서드 ==========

  @Transactional
  public InviteCodeGenerateResponse generateInviteCode(User user, Long workplaceId,
      InviteCodeGenerateRequest request) {
    if (!workplaceRepository.existsById(workplaceId)) {
      throw new WorkplaceNotFoundException();
    }
    permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplaceId);

    boolean returnAlreadyExists =
        !request.isForceGenerate() && inviteCodeService.existsByWorkplaceId(workplaceId);
    String inviteCode = inviteCodeService.generateInviteCode(workplaceId,
        request.isForceGenerate());

    return InviteCodeGenerateResponse.builder()
        .inviteCode(inviteCode)
        .returnAlreadyExists(returnAlreadyExists)
        .build();
  }

  @Transactional(readOnly = true)
  public InviteCodeInquiryResponse inquireInviteCode(User user, String inviteCode) {
    Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(inviteCode.toUpperCase());
    if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) {
      throw new WorkerAlreadyExistsException();
    }

    Workplace workplace = workplaceRepository.findById(workplaceId)
        .orElseThrow(WorkplaceNotFoundException::new);

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
    Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCode(
        request.getInviteCode().toUpperCase());
    if (!workplaceRepository.existsById(workplaceId)) {
      throw new WorkplaceNotFoundException();
    }
    if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) {
      throw new WorkerAlreadyExistsException();
    }

    Long ownerId = workplaceRepository.findOwnerId(workplaceId);

    // 푸시 알림 전달
    // TODO: JUnit으로 단위 테스트하기
    try {
      fcmService.sendToSingleUser(user.getId(), ownerId,
          // 제목: "근무지 참가 요청"
          // 본문: "{유저 이름}님이 근무지 참가 요청을 보냈습니다."
          AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REQUEST.toString(),
          AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REQUEST.getContent(user.getUsername()));
    } catch (FirebaseMessagingException e) {
      throw new CustomFirebaseMessagingException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

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
