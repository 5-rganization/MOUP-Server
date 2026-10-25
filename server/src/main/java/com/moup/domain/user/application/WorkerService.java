package com.moup.domain.user.application;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.exception.WorkerNotFoundException;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.user.dto.WorkerSummaryListResponse;
import com.moup.domain.user.dto.WorkerSummaryResponse;
import com.moup.domain.user.dto.WorkerWorkAttendanceResponse;
import com.moup.domain.user.dto.WorkerWorkerUpdateRequest;
import com.moup.global.infra.fcm.FCMService;
import com.moup.domain.user.dto.OwnerWorkerUpdateRequest;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.application.SalaryChangeNotifier;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.user.exception.WorkerStateConflictException;
import com.moup.domain.salary.application.SalaryUpdateService;
import com.moup.domain.salary.dto.SalaryDetailResponse;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.work.dto.MyAttendanceInfoResponse;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.work.dto.WorkerAttendanceInfoResponse;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.domain.alarm.domain.AlarmContent;
import com.moup.domain.alarm.domain.AlarmTitle;
import com.moup.global.error.CannotDeleteDataException;
import com.moup.global.error.ErrorCode;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.user.domain.User;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.global.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;
    private final UserRepository userRepository;

    private final PermissionVerifyUtil permissionVerifyUtil;
    private final WorkRepository workRepository;
    private final SalaryUpdateService salaryUpdateService;
    private final SalaryCalculationService salaryCalculationService;
    private final SalaryChangeNotifier salaryChangeNotifier;
    private final FCMService fCMService;

    public WorkerSummaryListResponse getWorkerList(Long userId, Long workplaceId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(
            WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        // 1. 사장님을 제외한 모든 근무자 조회
        List<Worker> workerList = workerRepository.findAllByWorkplaceIdAndUserIdNot(workplaceId, userId);

        // 2. User 맵을 만들기 위한 유효한 ID 목록
        List<Long> validUserIds = workerList.stream()
                .map(Worker::getUserId)
                .filter(Objects::nonNull)
                .toList();

        // 3. User 맵 생성
        Map<Long, User> userMap = validUserIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllByIdListIn(validUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. 근무자를 필터링하지 않고, map 내부에서 NULL 체크
        List<WorkerSummaryResponse> workerSummaryInfoList = workerList.stream()
                .map(worker -> {
                    // user_id가 NULL이 아니면 userMap에서 찾고, NULL이면 user도 null
                    User user = (worker.getUserId() != null)
                            ? userMap.get(worker.getUserId())
                            : null;

                    // User 객체가 null이면 (탈퇴했거나, DB 불일치) 기본값 사용
                    String nickname = (user != null) ? user.getNickname() : "탈퇴한 근무자";
                    String profileImg = (user != null) ? user.getProfileImg() : null; // 또는 기본 이미지 URL

                    return WorkerSummaryResponse.builder()
                            .workerId(worker.getId())
                            .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                            .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                            .nickname(nickname)
                            .profileImg(profileImg)
                            .isAccepted(worker.getIsAccepted())
                            .hasLeft(worker.hasLeft())
                            .build();
                })
                .toList();

        return WorkerSummaryListResponse.builder()
                .workerSummaryInfoList(workerSummaryInfoList)
                .build();
    }

    public WorkerSummaryListResponse getActiveWorkerList(Long userId, Long workplaceId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        // 1. 사장님을 제외한 모든 근무자 조회
        List<Worker> workerList = workerRepository.findAllByWorkplaceIdAndUserIdNot(workplaceId, userId);

        // 2. user_id가 NULL이 아닌 ID 목록만 추출
        List<Long> validUserIds = workerList.stream()
                .map(Worker::getUserId)
                .filter(Objects::nonNull)
                .toList();

        // 3. 유효한 ID로만 User 맵 생성
        if (validUserIds.isEmpty()) {
            return WorkerSummaryListResponse.builder()
                    .workerSummaryInfoList(Collections.emptyList())
                    .build();
        }

        Map<Long, User> userMap = userRepository.findAllByIdListIn(validUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. user_id가 NULL이 아니며, userMap에도 존재하는 근무자만 필터링
        List<WorkerSummaryResponse> workerSummaryInfoList = workerList.stream()
                // 활성 = 재직 중이고 탈퇴하지 않았다. 탈퇴는 가명처리라 user 행이 남으므로 맵에 있는지만 보면
                // 탈퇴자가 "탈퇴한 사용자"로 활성 목록에 나온다(스코프 4 탈퇴 신호 #3).
                .filter(worker -> !worker.hasLeft())
                .filter(worker -> worker.getUserId() != null && userMap.containsKey(worker.getUserId())
                        && !userMap.get(worker.getUserId()).isDeleted())
                .map(worker -> {
                    User user = userMap.get(worker.getUserId());
                    return WorkerSummaryResponse.builder()
                            .workerId(worker.getId())
                            .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                            .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                            .nickname(user.getNickname())
                            .profileImg(user.getProfileImg())
                            .isAccepted(worker.getIsAccepted())
                            .hasLeft(worker.hasLeft())
                            .build();
                })
                .toList();

        return WorkerSummaryListResponse.builder()
                .workerSummaryInfoList(workerSummaryInfoList)
                .build();
    }

    public MyAttendanceInfoResponse getMyAttendanceInfo(Long userId, Long workplaceId) {
        if (!workplaceRepository.existsById(workplaceId)) { throw new WorkplaceNotFoundException(); }
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(
            WorkerNotFoundException::new);

        List<WorkerWorkAttendanceResponse> workerWorkAttendanceInfoList = workRepository.findAllByWorkerId(userWorker.getId()).stream()
                .map(work -> WorkerWorkAttendanceResponse.builder()
                        .workId(work.getId())
                        .workDate(work.getWorkDate())
                        .startTime(work.getStartTime().atZone(SEOUL_ZONE_ID).toInstant())
                        .actualStartTime(work.getActualStartTime() != null ? work.getActualStartTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .endTime(work.getEndTime() != null ? work.getEndTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .actualEndTime(work.getActualEndTime() != null ? work.getActualEndTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .build())
                .toList();

        return MyAttendanceInfoResponse.builder()
                .myWorkAttendanceInfoList(workerWorkAttendanceInfoList)
                .build();
    }

    public WorkerAttendanceInfoResponse getWorkerAttendanceInfo(Long userId, Long workplaceId, Long workerId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        if (!workerRepository.existsByIdAndWorkplaceId(workerId, workplaceId)) { throw new WorkerNotFoundException(); }
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        List<WorkerWorkAttendanceResponse> workerWorkAttendanceInfoList = workRepository.findAllByWorkerId(workerId).stream()
                .map(work -> WorkerWorkAttendanceResponse.builder()
                        .workId(work.getId())
                        .workDate(work.getWorkDate())
                        .startTime(work.getStartTime().atZone(SEOUL_ZONE_ID).toInstant())
                        .actualStartTime(work.getActualStartTime() != null ? work.getActualStartTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .endTime(work.getEndTime() != null ? work.getEndTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .actualEndTime(work.getActualEndTime() != null ? work.getActualEndTime().atZone(SEOUL_ZONE_ID).toInstant() : null)
                        .build())
                .toList();

        // 사장님이 급여를 부분 수정하기 전에 현재 값을 볼 수 있어야 한다(스코프 3 I-8).
        SalaryDetailResponse salaryDetailInfo = salaryRepository.findByWorkerId(workerId)
                .map(SalaryDetailResponse::from)
                .orElse(null);

        return WorkerAttendanceInfoResponse.builder()
                .workplaceId(workplaceId)
                .workerId(workerId)
                .workerWorkAttendanceInfoList(workerWorkAttendanceInfoList)
                .salaryDetailInfo(salaryDetailInfo)
                .build();
    }

    @Transactional
    public void updateMyWorker(User user, Long workplaceId, WorkerWorkerUpdateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        // 정책 6 (b) — 자기 급여 설정은 승인 대기 중에도 본인이 다룰 수 있다.
        permissionVerifyUtil.verifyWorkerIdentityAllowingPending(user.getId(), userWorker.getUserId(), workplaceOwnerId);
        // 사장님이 탈퇴했거나 근무가 끝난 근무지에서는 급여 설정을 바꿀 수 없다 (확정 정책 5, D11-5).
        // 근무지 수정 경로에만 있고 이 경로에는 빠져 있어, 같은 변경을 URL만 바꿔 할 수 있었다(스코프 4 C4).
        permissionVerifyUtil.verifyWritable(userWorker, workplaceOwnerId);

        if (request.getWorkerBasedLabelColor() != null) {
            workerRepository.updateWorkerBasedLabelColor(userWorker.getId(), user.getId(), workplaceId, request.getWorkerBasedLabelColor());
        }
        if (request.getSalaryUpdateRequest() != null
                && salaryUpdateService.update(userWorker.getId(), request.getSalaryUpdateRequest())) {
            salaryChangeNotifier.notifyOwner(user, workplaceId);
        }
    }

    @Transactional
    public void updateWorkerForOwner(User user, Long workplaceId, Long workerId, OwnerWorkerUpdateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplace.getOwnerId());
        // 권한 검증 후 대상 근무자가 이 근무지 소속인지 확인한다. 없으면 남의 근무지 근무자의 급여를 덮어쓸 수 있다.
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        // 내보낸 알바생의 급여는 바꾸지 않는다. 라벨색도 같이 막는다 — 끝난 관계다.
        permissionVerifyUtil.verifyWritable(worker, workplace.getOwnerId());

        if (request.getOwnerBasedLabelColor() != null) {
            workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId, request.getOwnerBasedLabelColor());
        }
        // 확정 정책 2 — 사장님도 알바생 급여를 다룰 수 있다(Phase 11 D11-4). 보낸 필드만 바뀌고,
        // 현재 값은 GET /workplaces/{workplaceId}/workers/{workerId}의 salaryDetailInfo로 확인한다.
        if (request.getSalaryUpdateRequest() != null
                && salaryUpdateService.update(workerId, request.getSalaryUpdateRequest())) {
            salaryChangeNotifier.notifyWorker(user.getId(), worker.getUserId(), workplace);
        }
    }

    @Transactional
    public void deleteMyWorker(Long userId, Long workplaceId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        // 참여 취소는 승인 대기 중에도 가능해야 한다. 막으면 대기자가 근무지에서 빠져나올 수 없다.
        permissionVerifyUtil.verifyWorkerIdentityAllowingPending(userId, worker.getUserId(), workplaceOwnerId);

        workerRepository.delete(worker.getId(), worker.getUserId(), workplaceId);
    }

    /// 사장님이 근무자를 근무지에서 뺀다.
    ///
    /// 예전에는 무조건 `workers` 행을 지웠고, CASCADE로 **그 알바생의 근무·급여 이력이 전부 사라졌다**
    /// (스코프 4 I4). 이제 기록이 있으면 근무 관계만 끝내고 행은 남긴다(Phase 11 D11-5).
    /// 알바생은 그 근무지를 "근무 종료"로 보며 지난 기록을 읽기 전용으로 본다.
    /// 이미 끝난 근무자에게 다시 호출하면 아무것도 하지 않는다.
    @Transactional
    public void deleteWorkerForOwner(Long userId, Long workplaceId, Long workerId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(userId, workplaceOwnerId);

        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        // NULL-safe — 과거 하드 삭제로 user_id가 NULL인 행에서 NPE(500)가 났다(스코프 4 탈퇴 신호 #4).
        if (Objects.equals(worker.getUserId(), userId)) {
            throw new CannotDeleteDataException();
        }
        removeOrEndEmployment(worker);
    }

    @Transactional
    public void acceptWorker(Long ownerUserId, Long workplaceId, Long workerId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplace.getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(ownerUserId, workplaceOwnerId);
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new);

        if (worker.hasLeft()) {
            throw new WorkerStateConflictException("근무가 종료된 근무자입니다. 다시 참여 요청을 보내야 승인할 수 있습니다.");
        }
        // 멱등 — 이미 승인된 근무자를 다시 승인해도 아무것도 하지 않는다. 예전에는 누를 때마다 푸시가 다시 나갔다.
        if (Boolean.TRUE.equals(worker.getIsAccepted())) {
            return;
        }

        // 승인을 먼저 확정한다. 푸시는 best-effort이며 커밋 이후에 나간다.
        workerRepository.updateIsAccepted(workerId, worker.getUserId(), workplaceId, true);

        fCMService.sendToSingleUser(ownerUserId, worker.getUserId(), AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED.getTitle(), AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_ACCEPTED.getContent(workplace.getWorkplaceName()), null);
    }

    /// 참여 요청을 거절한다. **승인 대기자에게만** 쓴다.
    ///
    /// 예전에는 승인 여부를 보지 않고 행을 지워, 재직 중인 알바생에게 거절을 한 번 보내면
    /// 근무·급여 이력이 전부 사라졌다(스코프 4 I4). 재직자를 빼려면 근무자 삭제(근무 종료)를 쓴다.
    @Transactional
    public void rejectWorker(Long ownerUserId, Long workplaceId, Long workerId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(ownerUserId, workplace.getOwnerId());
        Worker worker = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new);

        if (Boolean.TRUE.equals(worker.getIsAccepted())) {
            throw new WorkerStateConflictException("이미 승인된 근무자는 거절할 수 없습니다. 근무를 끝내려면 근무자 삭제를 사용하세요.");
        }
        if (worker.hasLeft()) {
            throw new WorkerStateConflictException("이미 근무가 종료된 근무자입니다.");
        }
        removeOrEndEmployment(worker);

        // 푸시 알림 송신 (best-effort, 커밋 이후 발송)
        fCMService.sendToSingleUser(ownerUserId, worker.getUserId(), AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REJECTED.getTitle(), AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REJECTED.getContent(workplace.getWorkplaceName()), null);
    }

    /// 근무 기록이 없으면 행을 지우고, 있으면 근무 관계만 끝낸다.
    ///
    /// 승인 대기자라도 기록이 있을 수 있다 — 근무가 끝났다가 다시 참여 요청을 보낸 사람이다.
    /// 그 사람의 재참여를 거절했다고 지난 기록까지 지우면 안 된다.
    private void removeOrEndEmployment(Worker worker) {
        if (worker.hasLeft()) {
            return;
        }
        if (!workRepository.existsByWorkerId(worker.getId())) {
            workerRepository.deleteByIdAndWorkplaceId(worker.getId(), worker.getWorkplaceId());
            return;
        }
        if (Boolean.TRUE.equals(worker.getIsNowWorking())) {
            throw new WorkerStateConflictException("근무 중인 근무자는 내보낼 수 없습니다. 퇴근 처리 후 다시 시도해 주세요.");
        }

        // 오늘까지의 기록은 남기고, 다시 오지 않을 내일 이후 근무만 지운다.
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
        workRepository.deleteAllByWorkerIdAfterDate(worker.getId(), today);
        workerRepository.updateLeftAt(worker.getId(), worker.getWorkplaceId());

        // 이번 주 뒤쪽 근무가 사라졌으니 주휴수당과 이번 달 추정 실수령액이 달라진다.
        // 고정급은 recalculateWorkWeek이 조기 반환하므로 월 재계산을 따로 부른다.
        Salary salary = salaryRepository.findByWorkerId(worker.getId()).orElse(null);
        salaryCalculationService.recalculateWorkWeek(worker.getId(), today, salary);
        salaryCalculationService.recalculateEstimatedNetIncomeForMonth(
                worker.getId(), today.getYear(), today.getMonthValue(), salary);
    }

}
