package com.moup.server.service;

import com.moup.server.exception.CannotDeleteDataException;
import com.moup.server.exception.SalaryWorkerNotFoundException;
import com.moup.server.exception.WorkerWorkplaceNotFoundException;
import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import com.moup.server.repository.*;
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
    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final MonthlySalaryRepository monthlySalaryRepository;

    private final PermissionVerifyUtil permissionVerifyUtil;

    public WorkerSummaryListResponse getWorkerList(Long userId, Long workplaceId) {
        Workplace userWorkplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyOwnerPermission(userId, userWorkplace.getOwnerId());

        List<Worker> workerList = workerRepository.findAllByWorkplaceIdAndUserIdNot(workplaceId, userId);

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

    public void updateMyWorker(User user, Long workplaceId, WorkerWorkerUpdateRequest request) {
        Worker userWorker = workerRepository.findByUserIdAndWorkplaceId(user.getId(), workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyWorkerPermission(user.getId(), userWorker.getUserId());
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

    public void deleteMyWorker(Long userId, Long workplaceId) {
        Worker worker = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new);
        permissionVerifyUtil.verifyWorkerPermission(userId, worker.getUserId());

        workerRepository.delete(worker.getId(), worker.getUserId(), workplaceId);
    }

    public void deleteWorkerForOwner(Long userId, Long workplaceId, Long workerId) {
        Long workplaceOwnerId = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new).getOwnerId();
        permissionVerifyUtil.verifyOwnerPermission(userId, workplaceOwnerId);

        Long workerUserId = workerRepository.findByIdAndWorkplaceId(workerId, workplaceId).orElseThrow(WorkerWorkplaceNotFoundException::new).getUserId();
        if (workerUserId.equals(userId)) { throw new CannotDeleteDataException(); }

        workerRepository.delete(workerId, workerUserId, workplaceId);
    }
}
