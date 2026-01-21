package com.moup.domain.routine.api;

import com.moup.domain.routine.dto.RoutineCreateRequest;
import com.moup.domain.routine.dto.RoutineCreateResponse;
import com.moup.domain.routine.dto.RoutineDetailResponse;
import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.dto.RoutineSummaryListResponse;
import com.moup.domain.routine.dto.RoutineSummaryResponse;
import com.moup.domain.routine.dto.RoutineUpdateRequest;
import com.moup.domain.routine.dto.TodayRoutineResponse;
import com.moup.global.common.type.ViewType;
import com.moup.global.security.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/routines")
public class RoutineController implements RoutineSpecification {
    private final IdentityService identityService;
    private final RoutineService routineService;

    @Override
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

    @Override
    @GetMapping
    public ResponseEntity<?> getAllRoutine() {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse routineSummaryListResponse = routineService.getAllRoutine(userId);
        return ResponseEntity.ok().body(routineSummaryListResponse);
    }

    @Override
    @GetMapping("/today")
    public ResponseEntity<?> getAllTodayRoutine() {
        Long userId = identityService.getCurrentUserId();

        TodayRoutineResponse response = routineService.getAllTodayWorkplaceRoutineCount(userId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/{routineId}")
    public ResponseEntity<?> getRoutine(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();

        return switch (view) {
            case SUMMARY -> {
                RoutineSummaryResponse response = routineService.getRoutine(userId, routineId);
                yield ResponseEntity.ok().body(response);
            }
            case DETAIL -> {
                RoutineDetailResponse response = routineService.getRoutineDetail(userId, routineId);
                yield ResponseEntity.ok().body(response);
            }
        };
    }

    @Override
    @PatchMapping("/{routineId}")
    public ResponseEntity<?> updateRoutine(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @RequestBody @Valid RoutineUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        routineService.updateRoutine(userId, routineId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{routineId}")
    public ResponseEntity<?> deleteRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId) {
        Long userId = identityService.getCurrentUserId();

        routineService.deleteRoutine(userId, routineId);
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
