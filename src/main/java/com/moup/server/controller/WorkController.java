package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
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
        Long requesterId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createWorkForWorkerId(requesterId, workplaceId, workerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    public ResponseEntity<?> getWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        if (view == ViewType.SUMMARY) {
            WorkSummaryResponse response = workService.getSummarizedWork(userId, workplaceId, workerId, workId);
            return ResponseEntity.ok().body(response);
        }

        WorkDetailResponse response = workService.getWorkDetail(userId, workplaceId, workerId, workId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/works")
    public ResponseEntity<?> getSummarizedWorkForAllWorkplaces(
            @RequestParam(name = "baseYearMonth", required = false) YearMonth baseYearMonth
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCalendarListResponse response = workService.getAllSummarizedMyWorkForAllWorkplaces(userId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/workplaces/{workplaceId}/works")
    public ResponseEntity<?> getSummarizedWorkByWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "baseYearMonth", required = false) YearMonth baseYearMonth,
            @RequestParam(name = "isShared", required = false) Boolean isShared
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkCalendarListResponse response = workService.getAllSummarizedWorkByWorkplace(user, workplaceId, baseYearMonth, isShared);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/works/{workId}/routines")
    public ResponseEntity<?> getWorkAllRoutine(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    ) {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse response = routineService.getAllRoutineByWork(userId, workId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PutMapping("/workplaces/{workplaceId}/workers/me/works/{workId}")
    public ResponseEntity<?> updateMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateMyWork(userId, workplaceId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> updateWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateWorkForWorkerId(userId, workplaceId, workerId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/workplaces/{workplaceId}/workers/me/works/{workId}")
    public ResponseEntity<?> deleteMyWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteMyWork(userId, workplaceId, workId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    public ResponseEntity<?> deleteWorkForWorker(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteWorkForWorker(userId, workplaceId, workerId, workId);
        return ResponseEntity.noContent().build();
    }
}
