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
import org.springframework.web.bind.ServletRequestBindingException;
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
    @Operation(summary = "근무지/매장 생성", description = "사용자 역할에 따라 근무지/매장을 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무지/매장 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> createWorkplace(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지/매장 생성 요청 DTO",
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
                                        @ExampleObject(name = "사장님 매장 생성", summary = "사장님 매장 생성 요청 DTO",
                                                value = """
                                                        {
                                                            "workplaceName": "세븐일레븐 동탄중심상가점",
                                                            "categoryName": "편의점",
                                                            "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                            "latitude": 37.200089,
                                                            "longitude": 127.072006,
                                                            "ownerBasedLabelColor": "blue"
                                                        }
                                                        """)
                                }
                                )) @RequestBody @Valid BaseWorkplaceCreateRequest workplaceCreateRequest
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceCreateResponse response = workplaceService.createWorkplace(user, workplaceCreateRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkplaceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{workplaceId}")
    @Operation(summary = "근무지/매장 업데이트", description = "사용자 역할에 따라 근무지/매장 정보를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지/매장 업데이트 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지/매장", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 근무지/매장에 해당하는 근무자가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "근무지/매장에 해당하는 급여가 존재하지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> updateWorkplace(
            @Parameter(name = "workplaceId", description = "업데이트할 근무지 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지/매장 생성 요청 DTO",
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
                                    @ExampleObject(name = "사장님 매장 업데이트", summary = "사장님 매장 업데이트 요청 DTO",
                                            value = """
                                                    {
                                                        "workplaceName": "세븐일레븐 동탄중심상가점",
                                                        "categoryName": "편의점",
                                                        "address": "경기 화성시 동탄중심상가1길 8 1층",
                                                        "latitude": 37.200089,
                                                        "longitude": 127.072006,
                                                        "ownerBasedLabelColor": "blue"
                                                    }
                                                    """)
                            }
                            )) @RequestBody @Valid BaseWorkplaceUpdateRequest workplaceUpdateRequest
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.updateWorkplace(user, workplaceId, workplaceUpdateRequest);
        return ResponseEntity.noContent().build();
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

        WorkplaceSummaryListResponse workplaceSummaryListResponse = WorkplaceSummaryListResponse.builder()
                .workplaceSummaryList(summaryResponseList)
                .build();
        return ResponseEntity.ok().body(workplaceSummaryListResponse);
    }

    @DeleteMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 삭제", description = "삭제할 근무지(매장) ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 근무지(매장)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteWorkplace(
            @Parameter(name = "workplaceId", description = "삭제할 근무지 ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.deleteWorkplace(user.getId(), workplaceId);
        return ResponseEntity.noContent().build();
    }
}
