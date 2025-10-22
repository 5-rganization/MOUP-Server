package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkService;
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

    @Override
    @PostMapping("/workplaces/{workplaceId}/workers/me/works")
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
    @PostMapping("/workplaces/{workplaceId}/workers/{workerId}/works")
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
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        if (view == ViewType.SUMMARY) {
            WorkSummaryResponse response = workService.getWork(userId, workId);
            return ResponseEntity.ok().body(response);
        }

        WorkDetailResponse response = workService.getWorkDetail(userId, workId);
        return ResponseEntity.ok().body(response);
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
    public ResponseEntity<?> updateWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateWork(userId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/workplaces/{workplaceId}/workers/me/works")
    public ResponseEntity<?> updateActualStartTimeOrCreateWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();

        if (workService.updateActualStartTimeOrCreateWork(userId, workplaceId)) {
            return ResponseEntity.noContent().build();
        } else {
            WorkCreateRequest workCreateRequest = WorkCreateRequest.builder()
                    .routineIdList(Collections.emptyList())
                    .startTime(LocalDateTime.now())
                    .actualStartTime(LocalDateTime.now())
                    .endTime(null)
                    .actualEndTime(null)
                    .restTimeMinutes(0)
                    .memo(null)
                    .repeatDays(Collections.emptyList())
                    .repeatEndDate(null)
                    .build();

            WorkCreateResponse response = workService.createMyWork(userId, workplaceId, workCreateRequest);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(response.getWorkId())
                    .toUri();
            return ResponseEntity.created(location).body(response);
        }
    }

    @Override
    @DeleteMapping("/works/{workId}")
    public ResponseEntity<?> deleteWork(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteWork(userId, workId);
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
