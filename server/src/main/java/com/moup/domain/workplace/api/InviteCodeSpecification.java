package com.moup.domain.workplace.api;

import com.moup.domain.workplace.dto.InviteCodeGenerateRequest;
import com.moup.domain.workplace.dto.InviteCodeGenerateResponse;
import com.moup.domain.workplace.dto.InviteCodeInquiryResponse;
import com.moup.domain.workplace.dto.WorkplaceJoinRequest;
import com.moup.domain.workplace.dto.WorkplaceJoinResponse;
import com.moup.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import com.moup.domain.workplace.domain.InviteCodeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/workplaces")
public interface InviteCodeSpecification {
    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @PutMapping("/{workplaceId}/invite-code")
    @Operation(summary = "초대 코드 생성 (사장님 전용)", description = "근무지(매장) ID를 경로로 전달받아 초대 코드 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미 만들어진 초대 코드 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateResponse.class))),
            @ApiResponse(responseCode = "201", description = "새로운 초대 코드 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "요청 형식 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "초대 코드 생성 실패 — 잠시 후 다시 시도", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "초대 코드 생성을 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateRequest.class)))
    ResponseEntity<?> generateInviteCode(
            @Parameter(name = "workplaceId", description = "초대 코드를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid InviteCodeGenerateRequest request
    );

    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @GetMapping("/invite-codes/{inviteCode}")
    @Operation(summary = "초대 코드로 근무지 조회 (알바생 전용)", description = "초대 코드를 경로로 전달받아 근무지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeInquiryResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "초대 코드 형식 오류 (0·1·O·I는 쓰이지 않는다)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "초대 코드 조회 실패가 너무 많음 — 잠시 후 다시 시도", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> inquireInviteCode(
            @Parameter(name = "inviteCode", description = "조회할 초대 코드", example = "MUP234", required = true, in = ParameterIn.PATH)
            @PathVariable @Pattern(regexp = InviteCodeFormat.REGEX, message = InviteCodeFormat.MESSAGE) String inviteCode
    );

    @Tag(name = "Invite Code", description = "초대 코드 관리 API 엔드포인트")
    @PostMapping("/join")
    @Operation(summary = "초대 코드를 통해 근무지 참여 (알바생 전용)", description = "`body`에 포함된 초대 코드를 통해 근무지 참여")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무지 참여 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceJoinResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "초대 코드 형식 오류 (0·1·O·I는 쓰이지 않는다)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "초대 코드 조회 실패가 너무 많음 — 잠시 후 다시 시도", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무지 참여를 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceJoinRequest.class)))
    ResponseEntity<?> joinWorkplace(@RequestBody @Valid WorkplaceJoinRequest request);
}
