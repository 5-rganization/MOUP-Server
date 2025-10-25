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

@RequestMapping("/workplaces")
public interface WorkplaceSpecification {
    @Tag(name = "Workplace", description = "근무지(매장) 정보 관리 API 엔드포인트")
    @PostMapping
    @Operation(summary = "근무지(매장) 생성", description = "사용자 역할에 따라 근무지(매장)을 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "근무지(매장) 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceCreateResponse.class))),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 근무지(매장) 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> createWorkplace(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지(매장) 생성 요청 DTO",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = { WorkerWorkplaceCreateRequest.class, OwnerWorkplaceCreateRequest.class }),
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
                                                        "salaryCreateRequest":
                                                        {
                                                            "salaryType": "SALARY_MONTHLY",
                                                            "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                            "hourlyRate": 10030,
                                                            "salaryDate": 15,
                                                            "hasNationalPension": true,
                                                            "hasHealthInsurance": true,
                                                            "hasEmploymentInsurance": true,
                                                            "hasIndustrialAccident": true,
                                                            "hasIncomeTax": false,
                                                            "hasHolidayAllowance": false,
                                                            "hasNightAllowance": false
                                                        }
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
    );

    @Tag(name = "Workplace", description = "근무지(매장) 정보 관리 API 엔드포인트")
    @GetMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 조회", description = "조회할 근무지(매장)의 ID를 경로로 전달받아 조회 (기본적으로 상세 정보 반환, `view=summary` 파라미터 사용 시 요약 정보 반환)")
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
                                                        "salaryDetailInfo":
                                                        {
                                                            "salaryType": "SALARY_MONTHLY",
                                                            "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                            "hourlyRate": 10030,
                                                            "salaryDate": 15,
                                                            "hasNationalPension": true,
                                                            "hasHealthInsurance": true,
                                                            "hasEmploymentInsurance": true,
                                                            "hasIndustrialAccident": true,
                                                            "hasIncomeTax": false,
                                                            "hasHolidayAllowance": false,
                                                            "hasNightAllowance": false
                                                        }
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
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getWorkplace(
            @Parameter(name = "workplaceId", description = "조회할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "view", description = "조회 방식 (기본값: 상세 정보, `summary`: 요약 정보)", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"summary"}))
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    );

    @Tag(name = "Workplace", description = "근무지(매장) 정보 관리 API 엔드포인트")
    @GetMapping
    @Operation(summary = "모든 근무지(매장) 조회", description = "사용자의 모든 근무지(매장) 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 근무지(매장) 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkplaceSummaryListResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllWorkplace(
            @Parameter(name = "isShared", description = "공유 근무지(매장) 조회 여부", in = ParameterIn.QUERY)
            @RequestParam(name = "isShared", required = false, defaultValue = "false") boolean isShared
    );

    @Tag(name = "Workplace", description = "근무지(매장) 정보 관리 API 엔드포인트")
    @PatchMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 업데이트", description = "사용자 역할에 따라 근무지(매장) 정보를 업데이트")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 업데이트 성공"),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "사용자가 이미 등록한 근무지(매장) 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "유효하지 않은 필드값 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> updateWorkplace(
            @Parameter(name = "workplaceId", description = "업데이트할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근무지(매장) 생성 요청 DTO",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = { WorkerWorkplaceUpdateRequest.class, OwnerWorkplaceUpdateRequest.class }),
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
                                                        "salaryUpdateRequest":
                                                        {
                                                            "salaryType": "SALARY_MONTHLY",
                                                            "salaryCalculation": "SALARY_CALCULATION_HOURLY",
                                                            "hourlyRate": 10030,
                                                            "salaryDate": 15,
                                                            "hasNationalPension": true,
                                                            "hasHealthInsurance": true,
                                                            "hasEmploymentInsurance": true,
                                                            "hasIndustrialAccident": true,
                                                            "hasIncomeTax": false,
                                                            "hasHolidayAllowance": false,
                                                            "hasNightAllowance": false
                                                        }
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
    );

    @Tag(name = "Workplace", description = "근무지(매장) 정보 관리 API 엔드포인트")
    @DeleteMapping("/{workplaceId}")
    @Operation(summary = "근무지(매장) 삭제", description = "삭제할 근무지(매장) ID를 경로로 전달받아 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "근무지(매장) 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> deleteWorkplace(
            @Parameter(name = "workplaceId", description = "삭제할 근무지(매장) ID", example = "1", required = true, in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    );

    @Tag(name = "Work", description = "근무 정보 관리 API 엔드포인트")
    @GetMapping("/{workplaceId}/works")
    @Operation(summary = "특정 근무지(매장)의 모든 근무 범위 조회", description = "근무지(매장) ID를 경로로, 연-월을 매개변수로 전달받아 해당 날짜를 중간값으로 1년간 해당 근무지(매장)의 모든 근무를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkCalendarListResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 경로/매개변수 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 없는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청한 정보를 찾을 수 없음 (상세 내용은 메세지 참고)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getAllWorkByWorkplace(
            @Parameter(name = "workplaceId", description = "조회할 근무지(매장) ID", example = "1", in = ParameterIn.PATH)
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @Parameter(name = "baseYearMonth", description = "조회할 연-월 (yyyy-MM)", in = ParameterIn.QUERY, required = true)
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
            );
}
