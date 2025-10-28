package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.server.common.AlarmContent;
import com.moup.server.common.AlarmTitle;
import com.moup.server.exception.*;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import com.moup.server.repository.*;
import com.moup.server.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.moup.server.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;
    private final UserRepository userRepository;

    private final PermissionVerifyUtil permissionVerifyUtil;
    private final WorkRepository workRepository;
    private final FCMService fCMService;

    public WorkerSummaryListResponse getWorkerList(Long userId, Long workplaceId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        // 1. 사장님을 제외한 모든 근무자 조회
        List<Worker> workerList = workerRepository.findAllByWorkplaceIdAndUserIdNot(workplaceId, userId);

        // 2. User 맵을 만들기 위한 유효한 ID 목록
        List<Long> validUserIds = workerList.stream()
                .map(Worker::getUserId)
                .filter(Objects::nonNull)
                .toList();

        // 3. User 맵 생성
        Map<Long, User> userMap = userRepository.findAllByIdListIn(validUserIds).stream()
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
                .filter(Objects::nonNull) // user_id가 NULL인 worker 제외
                .toList();

        // 3. 유효한 ID로만 User 맵 생성
        Map<Long, User> userMap = userRepository.findAllByIdListIn(validUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. user_id가 NULL이 아니며, userMap에도 존재하는 근무자만 필터링
        List<WorkerSummaryResponse> workerSummaryInfoList = workerList.stream()
                .filter(worker -> worker.getUserId() != null && userMap.containsKey(worker.getUserId()))
                .map(worker -> {
                    User user = userMap.get(worker.getUserId());
                    return WorkerSummaryResponse.builder()
                            .workerId(worker.getId())
                            .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                            .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                            .nickname(user.getNickname())
                            .profileImg(user.getProfileImg())
                            .build();
                })
                .toList();

        return WorkerSummaryListResponse.builder()
                .workerSummaryInfoList(workerSummaryInfoList)
                .build();
    }

    public WorkerAttendanceInfoResponse getWorkerAttendanceInfo(Long userId, Long workplaceId, Long workerId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
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

        return WorkerAttendanceInfoResponse.builder()
                .workerId(workerId)
                .workerWorkAttendanceInfoList(workerWorkAttendanceInfoList)
                .build();
    }

    public void updateMyWorker(User user, Long workplaceId, WorkerWorkerUpdateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerNotFoundException::new);
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyWorkerPermission(user.getId(), userWorker.getUserId(), workplaceOwnerId);
        workerRepository.updateWorkerBasedLabelColor(userWorker.getId(), user.getId(), workplaceId, request.getWorkerBasedLabelColor());

        Long salaryId = salaryRepository.findByWorkerId(userWorker.getId()).orElseThrow(SalaryWorkerNotFoundException::new).getId();
        Salary newSalary = request.getSalaryUpdateRequest().toEntity(salaryId, userWorker.getId());

        salaryRepository.update(newSalary);
    }

    public void updateWorkerForOwner(User user, Long workplaceId, Long workerId, OwnerWorkerUpdateRequest request) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplaceOwnerId);
        workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId, request.getOwnerBasedLabelColor());

        Long salaryId = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new).getId();
        Salary newSalary = request.getSalaryUpdateRequest().toEntity(salaryId, workerId);

        salaryRepository.update(newSalary);
    }

    public void updateWorkerIsNowWorking(Long userId, Long workplaceId, Boolean isNowWorking) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);

        workerRepository.updateIsNowWorking(userWorker.getId(), userId, workplaceId, isNowWorking);
    }

    public void deleteMyWorker(Long userId, Long workplaceId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerNotFoundException::new);
        permissionVerifyUtil.verifyWorkerPermission(userId, worker.getUserId(), workplaceOwnerId);

        workerRepository.delete(worker.getId(), worker.getUserId(), workplaceId);
    }

    public void deleteWorkerForOwner(Long userId, Long workplaceId, Long workerId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(userId, workplaceOwnerId);

        Long workerUserId = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new).getUserId();
        if (workerUserId.equals(userId)) {
            throw new CannotDeleteDataException();
        }

        workerRepository.delete(workerId, workerUserId, workplaceId);
    }

    @Transactional
    public void acceptWorker(Long ownerUserId, Long workplaceId, Long workerId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        Long workplaceOwnerId = workplace.getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(ownerUserId, workplaceOwnerId);
        Long workerUserId = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new).getUserId();

        // 푸시 알림 송신
        try {
            fCMService.sendToSingleUser(ownerUserId, workerUserId, AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED.toString(), AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_ACCEPTED.getContent(workplace.getWorkplaceName()));
        } catch (FirebaseMessagingException e) {
            throw new CustomFirebaseMessagingException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        workerRepository.updateIsAccepted(workerId, workerUserId, workplaceId, true);
    }

    @Transactional
    public void rejectWorker(Long ownerUserId, Long workplaceId, Long workerId) {
        Long workerUserId = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerNotFoundException::new).getUserId();
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);

        deleteWorkerForOwner(ownerUserId, workplaceId, workerId);

        // 푸시 알림 송신
        try {
            fCMService.sendToSingleUser(ownerUserId, workerUserId, AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REJECTED.toString(), AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REJECTED.getContent(workplace.getWorkplaceName()));
        } catch (FirebaseMessagingException e) {
            throw new CustomFirebaseMessagingException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
