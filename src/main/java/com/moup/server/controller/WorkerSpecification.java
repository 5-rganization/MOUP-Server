package com.moup.server.controller;

import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
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

@RequestMapping("/workplaces/{workplaceId}/workers")
public interface WorkerSpecification {
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
}
