package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Collections;

@RestController
@Validated
@RequiredArgsConstructor
public class WorkController implements WorkSpecification {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkService workService;
    private final WorkerService workerService;

    @Override
    @PostMapping("/workplaces/{workplaceId}/workers/me/works")
    public ResponseEntity<?> createMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid MyWorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createMyWork(userId, workplaceId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/works/{id}")
                .buildAndExpand(response.getWorkIdList().get(0))
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @PostMapping("/workplaces/{workplaceId}/works/batch")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> createWorkForWorkers(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkersWorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkersWorkCreateResponse response = workService.createWorkForWorkerIdList(userId, workplaceId, request);

        if (response.getFailedWorkerInfoList() != null && !response.getFailedWorkerInfoList().isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/works/{id}")
                    .buildAndExpand(response.getSuccessWorkIdList().get(0))
                    .toUri();

            return ResponseEntity.created(location).body(response);
        }
    }

    @Override
    @GetMapping("/workplaces/{workplaceId}/workers/me/works")
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
    @GetMapping("/works/{workId}")
    public ResponseEntity<?> getWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        return switch (view) {
            case SUMMARY -> {
                WorkSummaryResponse response = workService.getWork(userId, workId);
                yield ResponseEntity.ok().body(response);
            }
            case DETAIL -> {
                WorkDetailResponse response = workService.getWorkDetail(userId, workId);
                yield ResponseEntity.ok().body(response);
            }
        };
    }

    @Override
    @GetMapping("/works")
    public ResponseEntity<?> getAllMyWork(@RequestParam(name = "baseYearMonth") YearMonth baseYearMonth) {
        Long userId = identityService.getCurrentUserId();

        WorkCalendarListResponse response = workService.getAllMyWork(userId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/workplaces/{workplaceId}/works")
    public ResponseEntity<?> getAllWorkByWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkCalendarListResponse response = workService.getAllWorkByWorkplace(user, workplaceId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PatchMapping("/works/{workId}")
    public ResponseEntity<?> updateMySingleWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid MyWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateMySingleWork(userId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/works/recurring/{workId}")
    public ResponseEntity<?> updateMyRecurringWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid MyWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkService.UpdateWorkResult result = workService.updateMyRecurringWork(userId, workId, request);
        WorkCreateResponse response = WorkCreateResponse.builder()
                .workIdList(result.resultingWorkIds())
                .build();
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PatchMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> updateSingleWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkerWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateSingleWorkForWorker(userId, workplaceId, workerId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/workplaces/{workplaceId}/workers/{workerId}/works/recurring/{workId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> updateRecurringWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkerWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkService.UpdateWorkResult result = workService.updateRecurringWorkForWorker(userId, workplaceId, workerId, workId, request);

        WorkCreateResponse response = WorkCreateResponse.builder()
                .workIdList(result.resultingWorkIds())
                .build();
        return ResponseEntity.ok().body(response);
    }

    @Override
    @DeleteMapping("/works/{workId}")
    public ResponseEntity<?> deleteWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteWork(userId, workId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/works/recurring/{workId}")
    public ResponseEntity<?> deleteRecurringWorkIncludingDate(
            @Parameter(name = "workId", description = "삭제할 기준 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteRecurringWorkIncludingDate(userId, workId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/workplaces/{workplaceId}/workers/me/works/start")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> updateActualStartTimeOrCreateWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();

        if (workService.updateActualStartTime(userId, workplaceId)) {
            // API 명세: 204 No Content (업데이트 성공)
            return ResponseEntity.noContent().build();
        } else {
            // API 명세: 201 Created (근무 생성 성공)
            Instant currentTime = Instant.now();
            MyWorkCreateRequest request = MyWorkCreateRequest.builder()
                    .routineIdList(Collections.emptyList())
                    .startTime(currentTime)
                    .actualStartTime(currentTime)
                    .endTime(null)
                    .actualEndTime(null)
                    .restTimeMinutes(0)
                    .memo(null)
                    .repeatDays(Collections.emptyList())
                    .repeatEndDate(null)
                    .build();

            WorkCreateResponse response = workService.createMyWork(userId, workplaceId, request);
            workerService.updateWorkerIsNowWorking(userId, workplaceId, true);
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/works/{id}")
                    .buildAndExpand(response.getWorkIdList().get(0))
                    .toUri();
            return ResponseEntity.created(location).body(response);
        }
    }

    @Override
    @PatchMapping("/workplaces/{workplaceId}/workers/me/works/end")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> updateWorkActualEndTime(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateActualEndTime(userId, workplaceId);
        return ResponseEntity.noContent().build();
    }
}