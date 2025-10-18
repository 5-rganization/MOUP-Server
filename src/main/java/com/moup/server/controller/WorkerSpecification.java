package com.moup.server.controller;

import com.moup.server.model.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RequestMapping("/workplaces/{workplaceId}/workers")
public interface WorkerSpecification {
    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PostMapping("/me/works")
    @Operation(summary = "근무지에 사용자 근무 생성", description = "근무지(매장) ID를 경로로 전달받아 해당 근무지(매장)에 사용자의 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateRequest.class)))
    ResponseEntity<?> createMyWork(
            @Parameter(name = "workplaceId", description = "근무를 생성할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkCreateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @PostMapping("/{workerId}/works")
    @Operation(summary = "근무자에게 근무 생성 (사장님 전용)", description = "매장 ID와 근무자 ID를 경로로 전달받아 해당 매장에 특정 근무자의 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateRequest.class)))
    ResponseEntity<?> createWorkForWorker(
            @Parameter(name = "workplaceId", description = "근무를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "근무를 생성할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkCreateRequest request
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/me/works")
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

    @Tag(name = "Worker", description = "근무자 정보 관리 API 엔드포인트")
    @GetMapping
    @Operation(summary = "매장의 근무자 조회 (사장님 전용)", description = "매장 ID를 경로로 전달받아 해당 매장의 모든 근무자를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무자 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerSummaryListResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getWorkerList(
            @Parameter(name = "workplaceId", description = "조회할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    );

    @Tag(name = "Worker", description = "근무자 정보 관리 API 엔드포인트")
    @PatchMapping("/me")
    @Operation(summary = "근무지에 사용자의 근무자 정보 업데이트 (알바생 전용)", description = "근무지 ID를 경로로 전달받아 해당 근무지에 사용자에 해당하는 근무자 정보를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무자 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무자 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerWorkerUpdateRequest.class)))
    ResponseEntity<?> updateMyWorker(
            @Parameter(name = "workplaceId", description = "조회할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkerWorkerUpdateRequest request
    );

    @Tag(name = "Worker", description = "근무자 정보 관리 API 엔드포인트")
    @PatchMapping("/{workerId}")
    @Operation(summary = "매장의 근무자 업데이트 (사장님 전용)", description = "매장 ID와 근무자 ID를 경로로 전달받아 해당 매장의 근무자를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무자 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무자 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = OwnerWorkerUpdateRequest.class)))
    ResponseEntity<?> updateWorkerForOwner(
            @Parameter(name = "workplaceId", description = "조회할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "업데이트할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid OwnerWorkerUpdateRequest request
    );

    @Tag(name = "Worker", description = "근무자 정보 관리 API 엔드포인트")
    @DeleteMapping("/me")
    @Operation(summary = "근무지에 사용자의 근무자 정보 삭제 (알바생 전용)", description = "근무지 ID를 경로로 전달받아 해당 근무지에 사용자에 해당하는 근무자 정보를 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무자 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteMyWorker(
            @Parameter(name = "workplaceId", description = "조회할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    );

    @Tag(name = "Worker", description = "근무자 정보 관리 API 엔드포인트")
    @DeleteMapping("/{workerId}")
    @Operation(summary = "매장의 근무자 삭제 (사장님 전용)", description = "매장 ID와 근무자 ID를 경로로 전달받아 해당 매장의 근무자를 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무자 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteWorkerForOwner(
            @Parameter(name = "workplaceId", description = "조회할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "삭제할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId
    );
}
