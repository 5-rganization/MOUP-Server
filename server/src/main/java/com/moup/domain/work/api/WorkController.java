package com.moup.domain.work.api;

import com.moup.domain.work.dto.WorkCreateResponse;
import com.moup.domain.work.dto.WorkDetailResponse;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.dto.WorkSummaryResponse;
import com.moup.domain.work.domain.WorkCalendarListResponse;
import com.moup.domain.work.dto.MyWorkCreateRequest;
import com.moup.domain.work.dto.MyWorkUpdateRequest;
import com.moup.global.security.IdentityService;
import com.moup.domain.user.application.UserService;
import com.moup.domain.user.application.WorkerService;
import com.moup.domain.user.dto.WorkerWorkUpdateRequest;
import com.moup.domain.user.dto.WorkersWorkCreateRequest;
import com.moup.domain.user.dto.WorkersWorkCreateResponse;
import com.moup.global.common.type.ViewType;
import com.moup.domain.user.domain.User;
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

        // 출근 처리 전체가 하나의 트랜잭션이어야 한다. 예전에는 여기서 세 번 나눠 호출해
        // 중간에 실패하면 "출근은 됐는데 퇴근이 안 되는" 상태로 영구히 고착됐다.
        WorkService.ClockInResult result = workService.clockIn(userId, workplaceId);

        if (!result.created()) {
            // API 명세: 204 No Content (예정된 근무에 출근 기록만 갱신)
            return ResponseEntity.noContent().build();
        }

        // API 명세: 201 Created (근무 생성 성공)
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/works/{id}")
                .buildAndExpand(result.response().getWorkIdList().get(0))
                .toUri();
        return ResponseEntity.created(location).body(result.response());
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
