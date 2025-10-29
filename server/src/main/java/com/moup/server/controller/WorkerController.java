package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/workplaces/{workplaceId}/workers")
public class WorkerController implements WorkerSpecification {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkerService workerService;

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> getWorkerList(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "isActiveOnly", required = false, defaultValue = "false") boolean isActiveOnly
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkerSummaryListResponse response;
        if (isActiveOnly) {
            response = workerService.getActiveWorkerList(userId, workplaceId);
        } else {
            response = workerService.getWorkerList(userId, workplaceId);
        }
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/{workerId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> getWorkerAttendanceInfo(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkerAttendanceInfoResponse response = workerService.getWorkerAttendanceInfo(userId, workplaceId, workerId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PatchMapping("/{workerId}/accept")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> acceptWorker(@PathVariable Long workplaceId, @PathVariable Long workerId) {
        Long ownerId = identityService.getCurrentUserId();

        workerService.acceptWorker(ownerId, workplaceId, workerId);

        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{workerId}/accept")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> rejectWorker(@PathVariable Long workplaceId, @PathVariable Long workerId) {
        Long ownerId = identityService.getCurrentUserId();

        workerService.rejectWorker(ownerId, workplaceId, workerId);

        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/me")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> updateMyWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkerWorkerUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workerService.updateMyWorker(user, workplaceId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{workerId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> updateWorkerForOwner(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid OwnerWorkerUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workerService.updateWorkerForOwner(user, workplaceId, workerId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> deleteMyWorker(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId) {
        Long userId = identityService.getCurrentUserId();

        workerService.deleteMyWorker(userId, workplaceId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{workerId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> deleteWorkerForOwner(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId
    ) {
        Long userId = identityService.getCurrentUserId();

        workerService.deleteWorkerForOwner(userId, workplaceId, workerId);
        return ResponseEntity.noContent().build();
    }
}
