package com.moup.server.controller;

import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
import com.moup.server.model.dto.WorkerSummaryListResponse;
import com.moup.server.service.IdentityService;
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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/workplaces/{workplaceId}/workers")
public class WorkerController implements WorkerSpecification {
    private final IdentityService identityService;
    private final WorkerService workerService;
    private final WorkService workService;

    @Override
    @PostMapping("/{workerId}/works")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> createWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkCreateRequest request
    ) {
        Long requesterId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createWorkForWorkerId(requesterId, workplaceId, workerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> getWorkerList(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId) {
        Long requesterId = identityService.getCurrentUserId();

        WorkerSummaryListResponse response = workerService.getWorkerList(requesterId, workplaceId);
        return ResponseEntity.ok().body(response);
    }
}
