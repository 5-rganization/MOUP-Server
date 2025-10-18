package com.moup.server.service;

import com.moup.server.common.Role;
import com.moup.server.exception.InvalidPermissionAccessException;
import com.moup.server.exception.SalaryWorkerNotFoundException;
import com.moup.server.exception.WorkerWorkplaceNotFoundException;
import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.UserRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import com.moup.server.util.PermissionVerifyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final SalaryRepository salaryRepository;

    private final PermissionVerifyUtil permissionVerifyUtil;
    private final UserRepository userRepository;

    public WorkerSummaryListResponse getWorkerList(Long userId, Long workplaceId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        List<Worker> workerList = workerRepository.findAllByWorkplaceId(workplaceId);

        Map<Long, User> userMap = userRepository.findAllByIdListIn(workerList.stream().map(Worker::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<WorkerSummaryResponse> workerSummaryInfoList = workerList.stream()
                .map(worker -> WorkerSummaryResponse.builder()
                        .workerId(worker.getId())
                        .workerBasedLabelColor(worker.getWorkerBasedLabelColor())
                        .ownerBasedLabelColor(worker.getOwnerBasedLabelColor())
                        .nickname(userMap.get(worker.getUserId()).getNickname())
                        .profileImg(userMap.get(worker.getUserId()).getProfileImg())
                        .build()
                )
                .toList();

        return WorkerSummaryListResponse.builder()
                .workerSummaryInfoList(workerSummaryInfoList)
                .build();
    }

    public void updateWorker(User user, Long workplaceId, Long workerId, BaseWorkerUpdateRequest request) {
        if (user.getRole() == Role.ROLE_OWNER && request instanceof OwnerWorkerUpdateRequest ownerRequest) {
            Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
            permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplaceOwnerId);
            workerRepository.updateOwnerBasedLabelColor(workerId, user.getId(), workplaceId, ownerRequest.getOwnerBasedLabelColor());
        } else if (user.getRole() == Role.ROLE_WORKER && request instanceof WorkerWorkerUpdateRequest workerRequest) {
            Long workerUserId = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
            permissionVerifyUtil.verifyWorkerPermission(user.getId(), workerUserId);
            workerRepository.updateWorkerBasedLabelColor(workerId, user.getId(), workplaceId, workerRequest.getWorkerBasedLabelColor());
        } else {
            throw new InvalidPermissionAccessException();
        }

        Long salaryId = salaryRepository.findByWorkerId(workerId).orElseThrow(SalaryWorkerNotFoundException::new).getId();
        Salary newSalary = request.getSalaryUpdateRequest().toEntity(salaryId, workerId);

        salaryRepository.update(newSalary);
    }

    public void deleteWorker(Long userId, Long workplaceId, Long workerId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        Long workerUserId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        permissionVerifyUtil.verifyWorkerOrOwnerPermission(userId, workerUserId, workplaceOwnerId);

        workerRepository.delete(workerId, workerUserId, workplaceId);
    }
}
