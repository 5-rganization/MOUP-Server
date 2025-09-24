package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Routine-Controller", description = "루틴 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routines")
public class RoutineController {
    private final IdentityService identityService;
    private final RoutineService routineService;

    @PostMapping()
    @Operation(summary = "루틴 생성", description = "사용자가 루틴 정보를 입력하여 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루틴 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateRequest.class)))
    public ResponseEntity<?> createRoutine(@RequestBody RoutineCreateRequest routineCreateRequest) {
        Long userId = identityService.getCurrentUserId();

        RoutineCreateResponse routineCreateResponse = routineService.createRoutine(userId, routineCreateRequest);
        return ResponseEntity.ok().body(routineCreateResponse);
    }

    @GetMapping("/summary")
    @Operation(summary = "모든 루틴 요약 조회", description = "사용자의 모든 루틴 조회 및 요약")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 루틴 조회 및 요약 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> summarizeAllRoutine() {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse routineSummaryListResponse = routineService.summarizeAllRoutine(userId);
        return ResponseEntity.ok().body(routineSummaryListResponse);
    }

    @PatchMapping()
    @Operation(summary = "루틴 업데이트", description = "사용자가 루틴 정보를 입력하여 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 업데이트 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineUpdateRequest.class)))
    public ResponseEntity<?> updateRoutine(@RequestBody RoutineUpdateRequest routineUpdateRequest) {
        Long userId = identityService.getCurrentUserId();

        routineService.updateRoutine(userId, routineUpdateRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{routineId}")
    @Operation(summary = "루틴 삭제", description = "삭제할 루틴 ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 삭제 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteRoutine(@PathVariable Long routineId) {
        Long userId = identityService.getCurrentUserId();

        routineService.deleteRoutine(userId, routineId);
        return ResponseEntity.noContent().build();
    }
}
