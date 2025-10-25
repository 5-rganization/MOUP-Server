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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;

@RestController
@Validated
@RequiredArgsConstructor
public class WorkController implements WorkSpecification {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkService workService;
    private final RoutineService routineService;
    private final WorkerService workerService;

    @Override
    @PostMapping("/workplaces/{workplaceId}/workers/me/works")
    public ResponseEntity<?> createMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid MyWorkCreateRequest request
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
    @PostMapping("/workplaces/{workplaceId}/workers/{workerId}/works")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> createWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkerWorkCreateRequest request
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
    @PatchMapping("/works/{workId}")
    public ResponseEntity<?> updateMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid MyWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkService.UpdateWorkResult result = workService.updateMyWork(userId, workId, request);

        if (result.recurringCreatedOrReplaced()) {
            // 반복 근무가 생성/대체된 경우: 200 OK + 새 ID 목록 반환
            WorkCreateResponse response = WorkCreateResponse.builder()
                    .workId(result.resultingWorkIds())
                    .build();
            return ResponseEntity.ok().body(response);
        } else {
            // 단일 근무만 업데이트된 경우: 204 No Content 반환
            return ResponseEntity.noContent().build();
        }
    }

    @Override
    @PatchMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> updateWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkerWorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkService.UpdateWorkResult result = workService.updateWorkForWorkerId(userId, workplaceId, workerId, workId, request);

        if (result.recurringCreatedOrReplaced()) {
            // 반복 근무가 생성/대체된 경우: 200 OK + 새 ID 목록 반환
            WorkCreateResponse response = WorkCreateResponse.builder()
                    .workId(result.resultingWorkIds())
                    .build();
            return ResponseEntity.ok().body(response);
        } else {
            // 단일 근무만 업데이트된 경우: 204 No Content 반환
            return ResponseEntity.noContent().build();
        }
    }

    @Override
    @PutMapping("/workplaces/{workplaceId}/workers/me/works/start")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> updateActualStartTimeOrCreateWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();

        if (workService.updateActualStartTime(userId, workplaceId)) {
            return ResponseEntity.noContent().build();
        } else {
            LocalDateTime currentDateTime = LocalDateTime.now();
            MyWorkCreateRequest request = MyWorkCreateRequest.builder()
                    .routineIdList(Collections.emptyList())
                    .startTime(currentDateTime)
                    .actualStartTime(currentDateTime)
                    .endTime(null)
                    .actualEndTime(null)
                    .restTimeMinutes(0)
                    .memo(null)
                    .repeatDays(Collections.emptyList())
                    .repeatEndDate(null)
                    .build();

            WorkCreateResponse response = workService.createMyWork(userId, workplaceId, request);
            workerService.updateWorkerIsNowWorking(userId, workplaceId, true);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(response.getWorkId())
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
    @GetMapping("/works/{workId}/routines")
    public ResponseEntity<?> getWorkAllRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId) {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse response = routineService.getAllRoutineByWork(userId, workId);
        return ResponseEntity.ok().body(response);
    }
}
