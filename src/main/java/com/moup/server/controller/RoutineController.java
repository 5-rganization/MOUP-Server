package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.service.IdentityService;
import com.moup.server.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class RoutineController {
    private final IdentityService identityService;
    private final RoutineService routineService;

    @PostMapping
    @Operation(summary = "루틴 생성", description = "사용자가 루틴 정보를 입력하여 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루틴 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateRequest.class)))
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
    @Operation(summary = "모든 루틴 요약 조회", description = "사용자의 모든 루틴 조회 및 요약")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 루틴 조회 및 요약 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> getAllSummarizedRoutine() {
        Long userId = identityService.getCurrentUserId();

        RoutineSummaryListResponse routineSummaryListResponse = routineService.getAllSummarizedRoutine(userId);
        return ResponseEntity.ok().body(routineSummaryListResponse);
    }

    @GetMapping("/{routineId}")
    @Operation(summary = "루틴 조회", description = "조회할 루틴 ID를 경로로 전달받아 조회 (기본적으로 상세 정보 반환, `?view=summary` 파라미터 사용 시 요약 정보 반환)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루틴 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = { RoutineDetailResponse.class, RoutineSummaryResponse.class }),
                            examples = {
                                    @ExampleObject(name = "상세 정보 조회 (기본값)", summary = "루틴 상세 정보",
                                            value = """
                                                    {
                                                        "routineId": 1,
                                                        "routineName": "오픈 루틴",
                                                        "alarmTime": "14:30",
                                                        "routineTaskList": [
                                                            {
                                                                "content": "바닥 청소",
                                                                "orderIndex": 0,
                                                                "isChecked": true
                                                            },
                                                            {
                                                                "content": "전자레인지 청소",
                                                                "orderIndex": 1,
                                                                "isChecked": false
                                                            }
                                                        ]
                                                    }
                                                    """),
                                    @ExampleObject(name = "요약 정보 조회 (`view=summary`)", summary = "루틴 요약 정보",
                                            value = """
                                                    {
                                                        "routineId": 1,
                                                        "routineName": "오픈 루틴",
                                                        "alarmTime": "14:30"
                                                    }
                                                    """)
                            })),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 매개변수", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> getRoutine(
            @Parameter(name = "routineId", description = "조회할 루틴 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해주세요.") Long routineId,
            @Parameter(name = "view", description = "조회 방식 (기본값: 상세 정보, `summary`: 요약 정보)", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"summary"}))
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
    @Operation(summary = "루틴 업데이트", description = "사용자가 루틴 정보를 입력하여 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 업데이트 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineUpdateRequest.class)))
    public ResponseEntity<?> updateRoutine(
            @Parameter(name = "routineId", description = "업데이트할 루틴 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해주세요.") Long routineId,
            @RequestBody @Valid RoutineUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        routineService.updateRoutine(userId, routineId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{routineId}")
    @Operation(summary = "루틴 삭제", description = "삭제할 루틴 ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 삭제 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해주세요.") Long routineId) {
        Long userId = identityService.getCurrentUserId();

        routineService.deleteRoutine(userId, routineId);
        return ResponseEntity.noContent().build();
    }
}
