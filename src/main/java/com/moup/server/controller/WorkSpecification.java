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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RequestMapping("/works")
public interface WorkSpecification {
    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/{workId}")
    @Operation(summary = "근무 조회", description = "조회할 근무 ID를 경로로 전달받아 조회 (기본적으로 상세 정보 반환, `?view=summary` 파라미터 사용 시 요약 정보 반환)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = { WorkDetailResponse.class, WorkSummaryResponse.class }),
                    examples = {
                            @ExampleObject(name = "상세 정보 조회 (기본값)", summary = "근무 상세 정보",
                                    value = """
                                            {
                                                "workId": 1,
                                                "workerSummaryInfo": {
                                                    "workerId": 1,
                                                    "workerBasedLabelColor": "RED",
                                                    "ownerBasedLabelColor": "BLUE",
                                                    "nickname": "김사장",
                                                    "profileImg": "https://moup-bucket.s3.ap-northeast-2.amazonaws.com/5aedbc811d19b48d5151c9d05b48fc6751be282f5e89f478a3b81dbc16e2ada7.png"
                                                },
                                                "workplaceSummaryInfo": {
                                                    "workplaceId": 1,
                                                    "workplaceName": "세븐일레븐 동탄중심상가점",
                                                    "isShared": true
                                                },
                                                "routineSummaryInfoList": [
                                                    {
                                                        "routineId": 1,
                                                        "routineName": "오픈 루틴",
                                                        "alarmTime": "08:00"
                                                    }
                                                ],
                                                "workDate": "2025-10-11",
                                                "startTime": "2025-10-11 08:30",
                                                "actualStartTime": "2025-10-11 08:35",
                                                "endTime": "2025-10-11 15:30",
                                                "actualEndTime": "2025-10-11 15:40",
                                                "restTimeMinutes": 30,
                                                "memo": "오늘 재고 정리하는 날",
                                                "repeatDays": [
                                                    "MONDAY",
                                                    "WEDNESDAY"
                                                ],
                                                "repeatEndDate": "2025-11-11",
                                                "isEditable": true
                                        }
                                        """),
                            @ExampleObject(name = "요약 정보 조회 (`view=summary`)", summary = "근무 요약 정보",
                                    value = """
                                            {
                                                "workId": 1,
                                                "workerSummaryInfo": {
                                                    "workerId": 1,
                                                    "workerBasedLabelColor": "RED",
                                                    "ownerBasedLabelColor": "BLUE",
                                                    "nickname": "김사장",
                                                    "profileImg": "https://moup-bucket.s3.ap-northeast-2.amazonaws.com/5aedbc811d19b48d5151c9d05b48fc6751be282f5e89f478a3b81dbc16e2ada7.png"
                                                },
                                                "workplaceSummaryInfo": {
                                                    "workplaceId": 1,
                                                    "workplaceName": "세븐일레븐 동탄중심상가점",
                                                    "isShared": true
                                                },
                                                "workDate": "2025-10-11",
                                                "startTime": "2025-10-11 08:30",
                                                "endTime": "2025-10-11 15:30",
                                                "workMinutes": 420,
                                                "restTimeMinutes": 30,
                                                "repeatDays": [
                                                    "MONDAY",
                                                    "WEDNESDAY"
                                                ],
                                                "repeatEndDate": "2025-11-11",
                                                "isEditable": true
                                            }
                                            """)
                    })),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getWork(
            @Parameter(name = "workId", description = "조회할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @Parameter(name = "view", description = "조회 방식 (기본값: 상세 정보, `summary`: 요약 정보)", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"summary"}))
            @RequestParam(name = "view", required = false) ViewType view
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping
    @Operation(summary = "사용자의 모든 근무 범위 조회", description = "연-월을 매개변수로 전달받아 해당 날짜를 중간값으로 1년간 사용자의 모든 근무를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCalendarListResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllSummarizedWork(
            @Parameter(name = "baseYearMonth", description = "조회할 연-월 (yyyy-MM)", in = ParameterIn.QUERY, required = true)
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PatchMapping("/{workId}")
    @Operation(summary = "근무 업데이트", description = "근무 ID를 경로로 전달받아 해당하는 근무를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkUpdateRequest.class)))
    ResponseEntity<?> updateWork(
            @Parameter(name = "workId", description = "업데이트할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkUpdateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @DeleteMapping("/{workId}")
    @Operation(summary = "근무 삭제", description = "근무 ID를 경로로 전달받아 해당하는 근무를 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteWork(
            @Parameter(name = "workId", description = "삭제할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    );

    @Tag(name = "Routine", description = "루틴 정보 관리 API 엔드포인트")
    @GetMapping("/{workId}/routines")
    @Operation(summary = "근무에 해당하는 루틴 요약 조회", description = "근무에 해당하는 루틴 조회 및 요약")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무에 해당하는 루틴 조회 및 요약 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getWorkAllRoutine(
            @Parameter(name = "workId", description = "루틴을 조회할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    );
}
