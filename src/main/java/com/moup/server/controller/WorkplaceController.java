package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkplaceService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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

    @PostMapping
    @Operation(summary = "근무지(매장) 생성", description = "사용자 역할에 따라 근무지(매장)을 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무지(매장) 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 근무지 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> createWorkplace(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지(매장) 생성 요청 DTO",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = {WorkerWorkplaceCreateRequest.class, OwnerWorkplaceCreateRequest.class}),
                                examples = {
                                        @ExampleObject(name = "알바생 근무지 생성", summary = "알바생 근무지 생성 요청 DTO",
                                                value = """
                                                        {
                                                            "workplaceName": "세븐일레븐 동탄중심상가점",
                                                            "categoryName": "편의점",
                                                            "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                            "latitude": 37.200089,
                                                            "longitude": 127.072006,
                                                            "workerBasedLabelColor": "RED",
                                                            "salaryType": "SALARY_MONTHLY",
                                                            "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                            "hourlyRate": 10030,
                                                            "salaryDate": 15,
                                                            "hasNationalPension": true,
                                                            "hasHealthInsurance": true,
                                                            "hasEmploymentInsurance": true,
                                                            "hasIndustrialAccident": true,
                                                            "hasIncomeTax": false,
                                                            "hasNightAllowance": false
                                                        }
                                                        """),
                                        @ExampleObject(name = "사장님 매장 생성", summary = "사장님 매장 생성 요청 DTO",
                                                value = """
                                                        {
                                                            "workplaceName": "세븐일레븐 동탄중심상가점",
                                                            "categoryName": "편의점",
                                                            "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                            "latitude": 37.200089,
                                                            "longitude": 127.072006,
                                                            "ownerBasedLabelColor": "BLUE"
                                                        }
                                                        """)
                                }
                                )) @RequestBody @Valid BaseWorkplaceCreateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceCreateResponse response = workplaceService.createWorkplace(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkplaceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "모든 근무지(매장) 요약 조회", description = "사용자의 모든 근무지(매장) 조회 및 요약")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 근무지(매장) 조회 및 요약 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> getAllSummarizedWorkplace() {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        List<WorkplaceSummaryResponse> summaryResponseList = workplaceService.getAllSummarizedWorkplace(user.getId());

        WorkplaceSummaryListResponse response = WorkplaceSummaryListResponse.builder()
                .workplaceSummaryList(summaryResponseList)
                .build();
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 조회", description = "조회할 근무지(매장)의 ID를 경로로 전달받아 조회 (기본적으로 상세 정보 반환, `?view=summary` 파라미터 사용 시 요약 정보 반환)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지(매장) 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = { WorkerWorkplaceDetailResponse.class, OwnerWorkplaceDetailResponse.class, WorkplaceSummaryResponse.class }),
                            examples = {
                                    @ExampleObject(name = "상세 정보 조회 (기본값) - 알바생", summary = "근무지 상세 정보",
                                            value = """
                                                    {
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "categoryName": "편의점",
                                                        "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                        "latitude": 37.200089,
                                                        "longitude": 127.072006,
                                                        "workerBasedLabelColor": "red",
                                                        "salaryType": "SALARY_MONTHLY",
                                                        "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                        "hourlyRate": 10030,
                                                        "salaryDate": 15,
                                                        "hasNationalPension": true,
                                                        "hasHealthInsurance": true,
                                                        "hasEmploymentInsurance": true,
                                                        "hasIndustrialAccident": true,
                                                        "hasIncomeTax": false,
                                                        "hasNightAllowance": false
                                                    }
                                                    """),
                                    @ExampleObject(name = "상세 정보 조회 (기본값) - 사장님", summary = "매장 상세 정보",
                                            value = """
                                                    {
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "categoryName": "편의점",
                                                        "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                        "latitude": 37.200089,
                                                        "longitude": 127.072006,
                                                        "ownerBasedLabelColor": "blue"
                                                    }
                                                    """),
                                    @ExampleObject(name = "요약 정보 조회 (`view=summary`)", summary = "근무지(매장) 요약 정보",
                                            value = """
                                                    {
                                                        "workplaceId": 1,
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "isShared": true
                                                    }
                                                    """)
                            })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지(매장)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 근무지(매장)에 해당하는 근무자가 존재하지 않음 - 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> getWorkplace(
            @Parameter(name = "workplaceId", description = "조회할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId,
            @Parameter(name = "view", description = "조회 방식 (기본값: 상세 정보, `summary`: 요약 정보)", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"summary"}))
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (view == ViewType.SUMMARY) {
            WorkplaceSummaryResponse response = workplaceService.getSummarizedWorkplace(userId, workplaceId);
            return ResponseEntity.ok().body(response);
        }

        BaseWorkplaceDetailResponse response = workplaceService.getWorkplaceDetail(user, workplaceId);
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 업데이트", description = "사용자 역할에 따라 근무지(매장) 정보를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 업데이트 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지(매장)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 근무지(매장)에 해당하는 근무자가 존재하지 않음 - 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "근무지(매장)에 해당하는 급여가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 근무지(매장) 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> updateWorkplace(
            @Parameter(name = "workplaceId", description = "업데이트할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지(매장) 생성 요청 DTO",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = {WorkerWorkplaceUpdateRequest.class, OwnerWorkplaceUpdateRequest.class}),
                            examples = {
                                    @ExampleObject(name = "알바생 근무지 업데이트", summary = "알바생 근무지 업데이트 요청 DTO",
                                            value = """
                                                    {
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "categoryName": "편의점",
                                                        "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                        "latitude": 37.200089,
                                                        "longitude": 127.072006,
                                                        "workerBasedLabelColor": "RED",
                                                        "salaryType": "SALARY_MONTHLY",
                                                        "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                        "hourlyRate": 10030,
                                                        "salaryDate": 15,
                                                        "hasNationalPension": true,
                                                        "hasHealthInsurance": true,
                                                        "hasEmploymentInsurance": true,
                                                        "hasIndustrialAccident": true,
                                                        "hasIncomeTax": false,
                                                        "hasNightAllowance": false
                                                    }
                                                    """),
                                    @ExampleObject(name = "사장님 매장 업데이트", summary = "사장님 매장 업데이트 요청 DTO",
                                            value = """
                                                    {
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "categoryName": "편의점",
                                                        "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                        "latitude": 37.200089,
                                                        "longitude": 127.072006,
                                                        "ownerBasedLabelColor": "BLUE"
                                                    }
                                                    """)
                            }
                            )) @RequestBody @Valid BaseWorkplaceUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.updateWorkplace(user, workplaceId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 삭제", description = "삭제할 근무지(매장) ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지(매장)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "근무지에 해당하는 근무자가 존재하지 않음 - 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteWorkplace(
            @Parameter(name = "workplaceId", description = "삭제할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.deleteWorkplace(user.getId(), workplaceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/invite-code/{workplaceId}")
    @Operation(summary = "초대 코드 생성", description = "근무지(매장) ID를 경로로 전달받아 초대 코드 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미 만들어진 초대 코드 존재"),
            @ApiResponse(responseCode = "201", description = "새로운 초대 코드 생성 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매장", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "초대 코드 생성을 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InviteCodeGenerateRequest.class)))
    public ResponseEntity<?> generateInviteCode(
            @Parameter(name = "workplaceId", description = "초대 코드를 생성할 매장 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId,
            @RequestBody @Valid InviteCodeGenerateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        InviteCodeGenerateResponse response = workplaceService.generateInviteCode(user, workplaceId, request);
        if (response.getReturnAlreadyExists()) {
            return ResponseEntity.ok().body(response);
        } else {
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/workplaces/invite-code/{inviteCode}")
                    .buildAndExpand(response.getInviteCode())
                    .toUri();
            return ResponseEntity.created(location).body(response);
        }
    }

    @GetMapping("/invite-code/{inviteCode}")
    @Operation(summary = "초대 코드로 근무지 조회", description = "초대 코드를 경로로 전달받아 근무지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지 조회 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> inquireInviteCode(
            @Parameter(name = "inviteCode", description = "조회할 초대 코드", example = "MUP234", required = true, in = ParameterIn.PATH)
            @PathVariable String inviteCode
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        InviteCodeInquiryResponse response = workplaceService.inquireInviteCode(user, inviteCode);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/invite-code/{inviteCode}")
    @Operation(summary = "초대 코드를 통해 근무지 참여", description = "초대 코드를 경로로 전달받아 근무지 참여")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무지 참여 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 근무자로 존재", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "근무지 참여를 위한 요청 데이터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceJoinRequest.class)))
    public ResponseEntity<?> joinWorkplace(
            @Parameter(name = "inviteCode", description = "참여할 근무지의 초대 코드", example = "MUP234", required = true, in = ParameterIn.PATH)
            @PathVariable String inviteCode,
            @RequestBody @Valid WorkplaceJoinRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceJoinResponse response = workplaceService.joinWorkplace(user, inviteCode, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/workplaces/{id}/workers/{workerId}")
                .buildAndExpand(response.getWorkplaceId(), response.getWorkerId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
