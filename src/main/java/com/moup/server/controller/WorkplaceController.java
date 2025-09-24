package com.moup.server.controller;

import com.moup.server.common.Role;
import com.moup.server.exception.InvalidRoleAccessException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author neoskyclad
 * <p>
 * 근무지 정보 관리를 위한 Controller
 */
@Tag(name = "Workplace-Controller", description = "근무지(매장) 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workplaces")
public class WorkplaceController {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkplaceService workplaceService;

    @PostMapping("/worker")
    @Operation(summary = "알바생 근무지 생성", description = "알바생이 근무지 및 급여 정보를 입력 하여 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무지 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerWorkplaceCreateRequest.class)))
    public ResponseEntity<?> createWorkerWorkplace(@RequestBody WorkerWorkplaceCreateRequest workerWorkplaceCreateRequest) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (user.getRole() == Role.ROLE_WORKER || user.getRole() == Role.ROLE_ADMIN) {
            WorkplaceCreateResponse workplaceCreateResponse = workplaceService.createWorkerWorkplace(user.getId(), workerWorkplaceCreateRequest);
            return ResponseEntity.ok().body(workplaceCreateResponse);
        } else {
            throw new InvalidRoleAccessException();
        }
    }

    @PatchMapping("/worker")
    @Operation(summary = "알바생 근무지 업데이트", description = "알바생이 근무지 및 급여 정보를 입력 하여 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지 업데이트 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 근무지에 해당하는 근무자가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "근무자에 해당하는 급여가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무지 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkerWorkplaceUpdateRequest.class)))
    public ResponseEntity<?> updateWorkerWorkplace(@RequestBody WorkerWorkplaceUpdateRequest workerWorkplaceUpdateRequest) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (user.getRole() == Role.ROLE_WORKER || user.getRole() == Role.ROLE_ADMIN) {
            workplaceService.updateWorkerWorkplace(user.getId(), workerWorkplaceUpdateRequest);
            return ResponseEntity.noContent().build();
        } else {
            throw new InvalidRoleAccessException();
        }
    }

    @PostMapping("/owner")
    @Operation(summary = "사장님 매장 생성", description = "사장님이 매장 정보를 입력 하여 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "매장 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = OwnerWorkplaceCreateRequest.class)))
    public ResponseEntity<?> createOwnerWorkplace(@RequestBody OwnerWorkplaceCreateRequest ownerWorkplaceCreateRequest) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (user.getRole() == Role.ROLE_OWNER || user.getRole() == Role.ROLE_ADMIN) {
            WorkplaceCreateResponse workplaceCreateResponse = workplaceService.createOwnerWorkplace(user.getId(), ownerWorkplaceCreateRequest);
            return ResponseEntity.ok().body(workplaceCreateResponse);
        } else {
            throw new InvalidRoleAccessException();
        }
    }

    @PatchMapping("/owner")
    @Operation(summary = "사장님 매장 업데이트", description = "사장님이 매장 정보를 입력 하여 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "매장 업데이트 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매장", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 매장에 해당하는 근무자가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "매장 업데이트를 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = OwnerWorkplaceUpdateRequest.class)))
    public ResponseEntity<?> updateOwnerWorkplace(@RequestBody OwnerWorkplaceUpdateRequest ownerWorkplaceUpdateRequest) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (user.getRole() == Role.ROLE_OWNER || user.getRole() == Role.ROLE_ADMIN) {
            workplaceService.updateOwnerWorkplace(user.getId(), ownerWorkplaceUpdateRequest);
            return ResponseEntity.noContent().build();
        } else {
            throw new InvalidRoleAccessException();
        }
    }

    @GetMapping("/summary")
    @Operation(summary = "모든 근무지(매장) 요약 조회", description = "현재 로그인된 사용자의 모든 근무지(매장) 조회 및 요약")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 근무지(매장) 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> summarizeAllWorkplace() {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        List<WorkplaceSummaryResponse> summaryResponseList = workplaceService.summarizeAllWorkplace(user.getId());

        WorkplaceSummaryListResponse workplaceSummaryListResponse = WorkplaceSummaryListResponse.builder()
                .workplaceSummaryResponseList(summaryResponseList)
                .build();
        return ResponseEntity.ok().body(workplaceSummaryListResponse);
    }

    @DeleteMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 삭제", description = "삭제할 근무지(매장) ID를 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지(매장)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteWorkplace(@PathVariable Long workplaceId) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.deleteWorkplace(user.getId(), workplaceId);
        return ResponseEntity.noContent().build();
    }
}
