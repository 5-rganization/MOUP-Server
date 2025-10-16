package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Routine-Controller", description = "루틴 정보 관리 API 엔드포인트")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/routines")
public class RoutineController implements RoutineSpecification {
    private final IdentityService identityService;
    private final RoutineService routineService;

    @PostMapping
    public ResponseEntity<?> createRoutine(@RequestBody @Valid RoutineCreateRequest request) {
        Long userId = identityService.getCurrentUserId();

        RoutineCreateResponse response = routineService.createRoutine(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getRoutineId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllSummarizedRoutine() {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse routineSummaryListResponse = routineService.getAllSummarizedRoutine(userId);
        return ResponseEntity.ok().body(routineSummaryListResponse);
    }

    @GetMapping("/{routineId}")
    public ResponseEntity<?> getRoutine(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        if (view == ViewType.SUMMARY) {
            RoutineSummaryResponse response = routineService.getSummarizedRoutine(userId, routineId);
            return ResponseEntity.ok().body(response);
        }

        RoutineDetailResponse response = routineService.getRoutineDetail(userId, routineId);
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/{routineId}")
    public ResponseEntity<?> updateRoutine(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @RequestBody @Valid RoutineUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        routineService.updateRoutine(userId, routineId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{routineId}")
    public ResponseEntity<?> deleteRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId) {
        Long userId = identityService.getCurrentUserId();

        routineService.deleteRoutine(userId, routineId);
        return ResponseEntity.noContent().build();
    }
}
