package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import com.moup.server.service.WorkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/works")
public class WorkController implements WorkSpecification {
    private final IdentityService identityService;
    private final WorkService workService;
    private final RoutineService routineService;

    @Override
    @GetMapping("/{workId}")
    public ResponseEntity<?> getWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        if (view == ViewType.SUMMARY) {
            WorkSummaryResponse response = workService.getSummarizedWork(userId, workId);
            return ResponseEntity.ok().body(response);
        }

        WorkDetailResponse response = workService.getWorkDetail(userId, workId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<?> getAllSummarizedWork(@RequestParam(name = "baseYearMonth") YearMonth baseYearMonth) {
        Long userId = identityService.getCurrentUserId();

        WorkCalendarListResponse response = workService.getAllWork(userId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/{workId}/routines")
    public ResponseEntity<?> getWorkAllRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId) {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse response = routineService.getAllRoutineByWork(userId, workId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PatchMapping("/{workId}")
    public ResponseEntity<?> updateWork(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        workService.updateWork(userId, workId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{workId}")
    public ResponseEntity<?> deleteWork(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId) {
        Long userId = identityService.getCurrentUserId();

        workService.deleteWork(userId, workId);
        return ResponseEntity.noContent().build();
    }
}
