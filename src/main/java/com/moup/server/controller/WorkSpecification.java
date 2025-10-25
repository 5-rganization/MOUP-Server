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

public interface WorkSpecification {
    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PostMapping("/workplaces/{workplaceId}/workers/me/works")
    @Operation(summary = "근무지(매장)에 사용자 근무 생성", description = "근무지(매장) ID를 경로로 전달받아 해당 근무지(매장)에 사용자의 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "사용자 근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MyWorkCreateRequest.class)))
    ResponseEntity<?> createMyWork(
            @Parameter(name = "workplaceId", description = "근무를 생성할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid MyWorkCreateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PostMapping("/workplaces/{workplaceId}/workers/{workerId}/works")
    @Operation(summary = "근무자에게 근무 생성 (사장님 전용)", description = "매장 ID와 근무자 ID를 경로로 전달받아 해당 매장에 특정 근무자의 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무자 근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerWorkCreateRequest.class)))
    ResponseEntity<?> createWorkForWorker(
            @Parameter(name = "workplaceId", description = "근무를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "근무를 생성할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkerWorkCreateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/workplaces/{workplaceId}/workers/me/works")
    @Operation(summary = "특정 근무지(매장)에서 사용자 근무 범위 조회", description = "근무지(매장) ID를 경로로, 연-월을 매개변수로 전달받아 해당 날짜를 중간값으로 1년간 해당 근무지(매장)에서 사용자의 근무를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCalendarListResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllMyWorkByWorkplace(
            @Parameter(name = "workplaceId", description = "조회할 근무지(매장) ID", example = "1", in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "baseYearMonth", description = "조회할 연-월 (yyyy-MM)", in = ParameterIn.QUERY, required = true)
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/works/{workId}")
    @Operation(summary = "근무 조회", description = "조회할 근무 ID를 경로로 전달받아 조회 (기본적으로 상세 정보 반환, `view=summary` 파라미터 사용 시 요약 정보 반환)")
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
                                                "isMyWork": true,
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
                                                "isMyWork": true,
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
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/works")
    @Operation(summary = "사용자의 모든 근무 범위 조회", description = "연-월을 매개변수로 전달받아 해당 날짜를 중간값으로 1년간 사용자의 모든 근무를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCalendarListResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllMyWork(
            @Parameter(name = "baseYearMonth", description = "조회할 연-월 (yyyy-MM)", in = ParameterIn.QUERY, required = true)
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PatchMapping("/works/{workId}")
    @Operation(summary = "사용자 근무 업데이트", description = "근무 ID를 경로로 전달받아 해당하는 근무를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 업데이트 및 반복 근무 생성/대체 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "204", description = "근무 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MyWorkUpdateRequest.class)))
    ResponseEntity<?> updateMyWork(
            @Parameter(name = "workId", description = "업데이트할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid MyWorkUpdateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PatchMapping("/workplaces/{workplaceId}/workers/{workerId}/works/{workId}")
    @Operation(summary = "근무자 근무 업데이트 (사장님 전용)", description = "매장 ID와 근무자 ID를 경로로 전달받아 해당 매장에 근무를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 업데이트 및 반복 근무 생성/대체 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "204", description = "근무 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerWorkUpdateRequest.class)))
    ResponseEntity<?> updateWorkForWorker(
            @Parameter(name = "workplaceId", description = "근무를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "근무를 생성할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @Parameter(name = "workId", description = "업데이트할 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId,
            @RequestBody @Valid WorkerWorkUpdateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @Operation(summary = "근무 출근 (알바생 전용)",
            description = "근무지 ID를 경로로 전달받아 출근 시간이 지금으로부터 과거 1시간 ~ 미래 1시간 사이에 해당하는 근무의 실제 출근 시간을 업데이트, 없으면 근무 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공"),
            @ApiResponse(responseCode = "204", description = "근무 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "근무자가 이미 근무중인 상태", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @PutMapping("/workplaces/{workplaceId}/workers/me/works/start")
    ResponseEntity<?> updateActualStartTimeOrCreateWork(
            @Parameter(name = "workplaceId", description = "출근할 근무지 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @Operation(summary = "근무 퇴근 (알바생 전용)", description = "근무지 ID를 경로로 전달받아 실제 퇴근 시간이 없는 마지막 근무의 실제 퇴근 시간을 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @PatchMapping("/workplaces/{workplaceId}/workers/me/works/end")
    ResponseEntity<?> updateWorkActualEndTime(
            @Parameter(name = "workplaceId", description = "퇴근할 근무지 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @DeleteMapping("/works/{workId}")
    @Operation(summary = "근무 삭제", description = "근무 ID를 경로로 전달받아 해당하는 근무를 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteWork(
            @Parameter(name = "workId", description = "삭제할 (기준) 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @DeleteMapping("/works/recurring/{workId}")
    @Operation(summary = "반복 근무 삭제", description = "기준 근무 ID를 경로로 전달받아 해당 근무 및 반복 그룹 내의 미래 근무를 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteRecurringWorkIncludingDate(
            @Parameter(name = "workId", description = "삭제할 기준 근무 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workId
    );

    @Tag(name = "Routine", description = "루틴 정보 관리 API 엔드포인트")
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
