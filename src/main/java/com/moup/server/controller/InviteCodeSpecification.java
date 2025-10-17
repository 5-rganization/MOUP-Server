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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface InviteCodeSpecification {
    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @GetMapping("/invite-codes/{inviteCode}")
    @Operation(summary = "초대 코드로 근무지 조회", description = "초대 코드를 경로로 전달받아 근무지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeInquiryResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> inquireInviteCode(
            @Parameter(name = "inviteCode", description = "조회할 초대 코드", example = "MUP234", required = true, in = ParameterIn.PATH)
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9]{6}$", message = "초대 코드는 영문 또는 숫자로 이루어진 6자리여야 합니다.") String inviteCode
    );

    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @PutMapping("/{workplaceId}/invite-code")
    @Operation(summary = "초대 코드 생성", description = "근무지(매장) ID를 경로로 전달받아 초대 코드 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미 만들어진 초대 코드 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateResponse.class))),
            @ApiResponse(responseCode = "201", description = "새로운 초대 코드 생성 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "초대 코드 생성을 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateRequest.class)))
    ResponseEntity<?> generateInviteCode(
            @Parameter(name = "workplaceId", description = "초대 코드를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid InviteCodeGenerateRequest request
    );

    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @PostMapping("/join")
    @Operation(summary = "초대 코드를 통해 근무지 참여", description = "`body`에 포함된 초대 코드를 통해 근무지 참여")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무지 참여 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceJoinResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무지 참여를 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceJoinRequest.class)))
    ResponseEntity<?> joinWorkplace(@RequestBody @Valid WorkplaceJoinRequest request);
}
