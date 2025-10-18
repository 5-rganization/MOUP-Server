package com.moup.server.service;

import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.dto.WorkerSummaryListResponse;
import com.moup.server.model.dto.WorkerSummaryResponse;
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
        permissionVerifyUtil.verifyWorkerServicePermission(userId, userWorkplace.getOwnerId());

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
}
