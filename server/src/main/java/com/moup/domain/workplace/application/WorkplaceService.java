package com.moup.domain.workplace.application;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.domain.workplace.dto.WorkplaceSummaryResponse;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.domain.WorkplaceStatus;
import com.moup.domain.workplace.domain.WorkplaceJoinPayload;
import com.moup.domain.workplace.dto.BaseWorkplaceCreateRequest;
import com.moup.domain.workplace.dto.BaseWorkplaceDetailResponse;
import com.moup.domain.workplace.dto.BaseWorkplaceUpdateRequest;
import com.moup.domain.workplace.dto.WorkplaceCreateResponse;
import com.moup.domain.workplace.dto.WorkplaceJoinRequest;
import com.moup.domain.workplace.dto.WorkplaceJoinResponse;
import com.moup.domain.workplace.exception.WorkplaceLimitExceededException;
import com.moup.domain.workplace.exception.WorkplaceNameAlreadyUsedException;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.infra.fcm.FCMService;
import com.moup.domain.workplace.dto.InviteCodeGenerateRequest;
import com.moup.domain.workplace.dto.InviteCodeGenerateResponse;
import com.moup.domain.workplace.dto.InviteCodeInquiryResponse;
import com.moup.domain.user.dto.OwnerWorkplaceCreateRequest;
import com.moup.domain.user.dto.OwnerWorkplaceDetailResponse;
import com.moup.domain.user.dto.OwnerWorkplaceUpdateRequest;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.dto.SalaryDetailResponse;
import com.moup.domain.user.dto.WorkerWorkplaceCreateRequest;
import com.moup.domain.user.dto.WorkerWorkplaceDetailResponse;
import com.moup.domain.user.dto.WorkerWorkplaceUpdateRequest;
import com.moup.domain.alarm.domain.AlarmContent;
import com.moup.domain.alarm.domain.AlarmTitle;
import com.moup.global.error.ErrorCode;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.domain.salary.exception.SalaryWorkerNotFoundException;
import com.moup.domain.user.exception.WorkerAlreadyExistsException;
import com.moup.domain.user.exception.WorkerNotFoundException;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.global.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;
    private final WorkRepository workRepository;

    private final InviteCodeService inviteCodeService;
    private final SalaryCalculationService salaryCalculationService;
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
                        .hasHolidayAllowance(salary.getHasHolidayAllowance())
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
        // 정책 6 (a) — 승인 대기 중에도 근무지 이름은 볼 수 있어야 한다.
        // 차단하는 대신 status로 대기 상태임을 알린다.
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new);

        return WorkplaceSummaryResponse.builder()
                .workplaceId(workplaceId)
                .workplaceName(workplace.getWorkplaceName())
                .isShared(workplace.isShared())
                .status(resolveStatus(workplace, worker))
                .build();
    }

    /// 알바생 시점의 근무지 상태를 판정한다.
    ///
    /// `owner_id`가 NULL이면 사장님이 하드 삭제된 것이다(FK가 `ON DELETE SET NULL`).
    /// 별도 컬럼 없이 이 값 하나로 "사장님 탈퇴"를 알 수 있다.
    private WorkplaceStatus resolveStatus(Workplace workplace, Worker worker) {
        if (workplace.getOwnerId() == null) {
            return WorkplaceStatus.OWNER_WITHDRAWN;
        }
        if (worker != null && !Boolean.TRUE.equals(worker.getIsAccepted())) {
            return WorkplaceStatus.PENDING_APPROVAL;
        }
        return WorkplaceStatus.ACTIVE;
    }

    @Transactional(readOnly = true)
    public List<WorkplaceSummaryResponse> getAllWorkplace(Long userId, boolean isSharedOnly) {
        // 1. [쿼리 1] 사용자가 속한 모든 Worker 정보를 가져온다.
        List<Worker> userAllWorkers = workerRepository.findAllByUserId(userId);

        // 2. Worker 정보에서 workplaceId 리스트를 추출한다. (중복 제거)
        List<Long> workplaceIds = userAllWorkers.stream()
                .map(Worker::getWorkplaceId)
                .distinct()
                .toList();

        if (workplaceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. [쿼리 2] workplaceId 리스트로 모든 Workplace 정보를 '한 번에' 가져온다.
        List<Workplace> workplaces = workplaceRepository.findAllByIdListIn(workplaceIds);

        // 4. 이제 DB 조회가 아닌 '메모리'에서 필터링 및 DTO 변환을 수행한다.
        Map<Long, Worker> workerByWorkplaceId = userAllWorkers.stream()
                .collect(Collectors.toMap(Worker::getWorkplaceId, w -> w, (a, b) -> a));

        return workplaces.stream()
                .filter(workplace -> !isSharedOnly || workplace.isShared())
                .map(workplace -> WorkplaceSummaryResponse.builder()
                        .workplaceId(workplace.getId())
                        .workplaceName(workplace.getWorkplaceName())
                        .isShared(workplace.isShared())
                        .status(resolveStatus(workplace, workerByWorkplaceId.get(workplace.getId())))
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
                updateWorkplaceFields(user.getId(), workplaceId, ownerRequest);
                Long workerId = findWorkerId(user.getId(), workplaceId);
                workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId,
                        ownerRequest.getOwnerBasedLabelColor());
            }
            case ROLE_WORKER -> {
                if (!(request instanceof WorkerWorkplaceUpdateRequest workerRequest)) {
                    throw new InvalidPermissionAccessException();
                }
                Long workerId = findWorkerId(user.getId(), workplaceId);
                workerRepository.updateWorkerBasedLabelColor(workerId, user.getId(), workplaceId,
                        workerRequest.getWorkerBasedLabelColor());

                Long salaryId = salaryRepository.findByWorkerId(workerId)
                        .orElseThrow(SalaryWorkerNotFoundException::new).getId();
                Salary newSalary = workerRequest.getSalaryUpdateRequest().toEntity(salaryId, workerId);
                salaryRepository.update(newSalary);

                // '현재' 및 '미래'의 모든 근무 월을 재계산합니다.

                // 재계산 기준일 (이번 달 1일) - ⭐️ SEOUL_ZONE_ID 적용
                LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
                LocalDate startDate = today.withDayOfMonth(1);

                // DB에서 'yyyy-MM-01' 이후로 근무가 잡힌 '모든 고유한 년/월' 목록 조회 (예: [2025-11], [2025-12])
                List<WorkRepository.WorkMonthDto> monthsToRecalculate = workRepository.findDistinctWorkMonthsAfter(
                        workerId, startDate);

                // 각 '연/월'별로 재계산을 실행합니다.
                for (WorkRepository.WorkMonthDto monthInfo : monthsToRecalculate) {
                    // ⭐️ [수정 포인트] 4번째 인자로 'newSalary' 전달
                    salaryCalculationService.recalculateEstimatedNetIncomeForMonth(
                            workerId,
                            monthInfo.year(),
                            monthInfo.month(),
                            newSalary
                    );
                }
            }
            case ROLE_ADMIN -> throw new InvalidPermissionAccessException();
        }
    }

    @Transactional
    public void deleteWorkplace(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);
        if (Objects.equals(workplace.getOwnerId(), userId)) {
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

        if (workplaceRepository.getOwnedWorkplaceCountByUserId(userId) >= workplaceCreationLimit) {
            throw new WorkplaceLimitExceededException(ErrorCode.WORKPLACE_LIMIT_EXCEEDED);
        }

        Workplace workplaceToCreate = request.toWorkplaceEntity(userId);
        workplaceRepository.create(workplaceToCreate);

        Worker workerToCreate = request.toWorkerEntity(userId, workplaceToCreate.getId());
        workerRepository.create(workerToCreate);

        return workerToCreate;
    }

    /// 근무지 필드 수정은 사장님 경로에서만 호출한다.
    ///
    /// `UPDATE workplaces ... WHERE id = ? AND owner_id = ?`이라 알바생이 호출하면
    /// 0행 갱신 후 204가 나갔다 — 아무것도 안 됐는데 성공으로 보이는 조용한 무시였다.
    /// 중복 이름 검사도 알바생의 userId를 owner_id로 넣어, 같은 이름의 매장을 따로
    /// 소유한 알바생이 라벨 색상만 바꿔도 409를 맞을 수 있었다. 이제 알바생 경로는
    /// 이 SQL들을 아예 실행하지 않는다.
    private void updateWorkplaceFields(Long ownerId, Long workplaceId, BaseWorkplaceUpdateRequest request) {
        Workplace oldWorkplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);

        // 이름을 생략했다면 바뀌지 않으므로 중복 검사도 필요 없다.
        if (request.getWorkplaceName() != null
                && !oldWorkplace.getWorkplaceName().equals(request.getWorkplaceName())
                && workplaceRepository.existsByOwnerIdAndWorkplaceName(ownerId,
                request.getWorkplaceName())) {
            throw new WorkplaceNameAlreadyUsedException();
        }

        Workplace newWorkplace = request.toWorkplaceEntity(workplaceId, ownerId);
        // 다섯 필드를 모두 생략하면(예: 라벨 색상만 변경) 동적 SQL의 <set>이 비어
        // `UPDATE workplaces WHERE ...`라는 깨진 문장이 된다. 아예 실행하지 않는다.
        if (newWorkplace.getWorkplaceName() == null && newWorkplace.getCategoryName() == null
                && newWorkplace.getAddress() == null
                && newWorkplace.getLatitude() == null && newWorkplace.getLongitude() == null) {
            return;
        }

        workplaceRepository.update(newWorkplace);
    }

    private Long findWorkerId(Long userId, Long workplaceId) {
        return workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerNotFoundException::new).getId();
    }

    // ========== 초대 코드 메서드 ==========

    @Transactional
    public InviteCodeGenerateResponse generateInviteCode(User user, Long workplaceId, InviteCodeGenerateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplace.getOwnerId());

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
        // 가장 싼 오라클이다 — body 없이 200(존재)/404(부재)로 즉시 갈린다.
        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(
                user.getId(), inviteCode.toUpperCase());
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
        Long workplaceId = inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(
                user.getId(), request.getInviteCode().toUpperCase());
        if (!workplaceRepository.existsById(workplaceId)) { throw new WorkplaceNotFoundException(); }
        if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }

        Long ownerId = workplaceRepository.findOwnerId(workplaceId);

        // 근무자 정보 생성
        Worker worker = request.toWorkerEntity(user.getId(), workplaceId);
        workerRepository.create(worker);

        // 급여 정보 생성
        Salary salary = request.toSalaryEntity(worker.getId());
        salaryRepository.create(salary);

        // 푸시 알림 전달 (best-effort, 커밋 이후 발송)
        // 제목: "근무지 참가 요청" / 본문: "{유저 이름}님이 근무지 참가 요청을 보냈습니다."
        String notificationContent = AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REQUEST.getContent(user.getUsername());
        String notificationTitle = AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REQUEST.getTitle();
        WorkplaceJoinPayload dataPayload = WorkplaceJoinPayload.builder()
                .content(notificationContent)
                .workplaceId(workplaceId)
                .workerId(worker.getId()).build();

        fcmService.sendToSingleUser(user.getId(), ownerId, notificationTitle, notificationContent, dataPayload);

        return WorkplaceJoinResponse.builder()
                .workplaceId(workplaceId)
                .workerId(worker.getId())
                .build();
    }
}
