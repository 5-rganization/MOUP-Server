package com.moup.server.controller;

import com.moup.server.model.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/routines")
public interface RoutineSpecification {
    @PostMapping
    @Operation(summary = "루틴 생성", description = "사용자가 루틴 정보를 입력하여 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루틴 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 루틴 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineCreateRequest.class)))
    ResponseEntity<?> createRoutine(@RequestBody @Valid RoutineCreateRequest request);

    @GetMapping
    @Operation(summary = "모든 루틴 조회", description = "사용자의 모든 루틴 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 루틴 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllRoutine();

    @GetMapping("/today")
    @Operation(summary = "오늘 루틴 조회", description = "사용자의 오늘 근무에 해당하는 루틴 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오늘 근무에 해당하는 루틴 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TodayRoutineResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllTodayRoutine();

    @GetMapping("/{routineId}")
    @Operation(summary = "루틴 조회", description = "조회할 루틴 ID를 경로로 전달받아 조회 (`view=detail`: 상세 정보 반환 - 기본값, `view=summary`: 요약 정보 반환)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루틴 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = { RoutineDetailResponse.class, RoutineSummaryResponse.class }),
                            examples = {
                                    @ExampleObject(name = "상세 정보 조회 (`view=detail` - 기본값)", summary = "루틴 상세 정보",
                                            value = """
                                                    {
                                                        "routineId": 1,
                                                        "routineName": "오픈 루틴",
                                                        "alarmTime": "14:30",
                                                        "routineTaskList": [
                                                            {
                                                                "content": "바닥 청소",
                                                                "orderIndex": 0
                                                            },
                                                            {
                                                                "content": "전자레인지 청소",
                                                                "orderIndex": 1
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
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getRoutine(
            @Parameter(name = "routineId", description = "조회할 루틴 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @Parameter(name = "view", description = "조회 방식 (기본값: 상세 정보, `summary`: 요약 정보)", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"summary"}))
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    );

    @PatchMapping("/{routineId}")
    @Operation(summary = "루틴 업데이트", description = "사용자가 루틴 정보를 입력하여 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 업데이트 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 루틴 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "루틴 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineUpdateRequest.class)))
    ResponseEntity<?> updateRoutine(
            @Parameter(name = "routineId", description = "업데이트할 루틴 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId,
            @RequestBody @Valid RoutineUpdateRequest request
    );

    @DeleteMapping("/{routineId}")
    @Operation(summary = "루틴 삭제", description = "삭제할 루틴 ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루틴 삭제 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루틴", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteRoutine(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long routineId);

    @GetMapping("/works/{workId}/routines")
    @Operation(summary = "근무에 해당하는 루틴 조회", description = "근무에 해당하는 루틴 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무에 해당하는 루틴 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getWorkAllRoutine(
            @Parameter(name = "workId", description = "루틴을 조회할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    );
}
