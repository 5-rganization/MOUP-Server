package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkService;
import com.moup.server.service.WorkerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/workplaces/{workplaceId}/workers")
public class WorkerController implements WorkerSpecification {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkerService workerService;
    private final WorkService workService;

    @Override
    @PostMapping("/me/works")
    public ResponseEntity<?> createMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createMyWork(userId, workplaceId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @PostMapping("/{workerId}/works")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> createWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createWorkForWorkerId(userId, workplaceId, workerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping("/me/works")
    public ResponseEntity<?> getAllMyWorkByWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkCalendarListResponse response = workService.getAllMyWorkByWorkplace(user, workplaceId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> getWorkerList(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId) {
        Long userId = identityService.getCurrentUserId();

        WorkerSummaryListResponse response = workerService.getWorkerList(userId, workplaceId);
        return ResponseEntity.ok().body(response);
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
