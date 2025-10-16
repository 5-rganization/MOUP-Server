package com.moup.server.controller;

import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
import com.moup.server.service.IdentityService;
import com.moup.server.service.WorkService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Work-Controller", description = "근무 정보 관리 API 엔드포인트")
@RestController
@Validated
@RequiredArgsConstructor
public class WorkController {
    private final IdentityService identityService;
    private final WorkService workService;

    @PostMapping("/workplaces/{workplaceId}/workers/me/works")
    @Operation(summary = "내 근무 생성", description = "근무지(매장)에 내 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "올바르지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateRequest.class)))
    public ResponseEntity<?> createMyWork(
            @Parameter(name = "workplaceId", description = "근무를 생성할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid WorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createWork(userId, workplaceId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/workplaces/{workplaceId}/workers/{workerId}/works")
    @Operation(summary = "근무자에게 근무 생성 (사장님 전용)", description = "매장에 특정 근무자의 근무를 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "올바르지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCreateRequest.class)))
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> createWorkForWorker(
            @Parameter(name = "workplaceId", description = "근무를 생성할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "workerId", description = "근무를 생성할 근무자 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workerId,
            @RequestBody @Valid WorkCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();

        WorkCreateResponse response = workService.createWorkForWorkerId(userId, workplaceId, workerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
