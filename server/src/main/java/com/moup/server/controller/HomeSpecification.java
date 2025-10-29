package com.moup.server.controller;

import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.OwnerHomeResponse;
import com.moup.server.model.dto.WorkerHomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/home")
public interface HomeSpecification {
    @GetMapping
    @Operation(summary = "홈 화면 정보 조회", description = "사용자 역할에 맞는 홈 화면 정보를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 화면 정보 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = { WorkerHomeResponse.class, OwnerHomeResponse.class }),
                            examples = {
                                    @ExampleObject(name = "알바생 홈 화면 조회", summary = "알바생 홈 화면 정보",
                                            value = """
                                                    {
                                                        "nowMonth": 9,
                                                        "totalSalary": 2743600,
                                                        "prevMonthSalaryDiff": 243600,
                                                        "todayRoutineCounts": 3,
                                                        "workerMonthlyWorkplaceSummaryInfoList": [
                                                            {
                                                                "homeWorkplaceSummaryInfo": {
                                                                    "workplaceSummaryInfo": {
                                                                        "workplaceId": 1,
                                                                        "workplaceName": "GS25 역삼점",
                                                                        "isShared": false
                                                                    },
                                                                    "isNowWorking": false
                                                                },
                                                                "daysUntilPayday": 14,
                                                                "totalWorkMinutes": 5280,
                                                                "dayTimeMinutes": 5280,
                                                                "nightTimeMinutes": 0,
                                                                "restTimeMinutes": 360,
                                                                "dayTimeIncome": 880000,
                                                                "totalHolidayAllowance": 80000,
                                                                "totalNightAllowance": 0,
                                                                "grossIncome": 960000,
                                                                "nationalPension": 43200,
                                                                "healthInsurance": 34560,
                                                                "employmentInsurance": 8640,
                                                                "incomeTax": 26400,
                                                                "netIncome": 847200
                                                            },
                                                            {
                                                                "homeWorkplaceSummaryInfo": {
                                                                    "workplaceSummaryInfo": {
                                                                        "workplaceId": 2,
                                                                        "workplaceName": "메가커피 선릉점",
                                                                        "isShared": true
                                                                    },
                                                                    "isNowWorking": false
                                                                },
                                                                "daysUntilPayday": 29,
                                                                "totalWorkMinutes": 10560,
                                                                "dayTimeMinutes": 9600,
                                                                "nightTimeMinutes": 960,
                                                                "restTimeMinutes": 1320,
                                                                "dayTimeIncome": 2120000,
                                                                "totalHolidayAllowance": 0,
                                                                "totalNightAllowance": 80000,
                                                                "grossIncome": 2200000,
                                                                "nationalPension": 99000,
                                                                "healthInsurance": 96800,
                                                                "employmentInsurance": 19800,
                                                                "incomeTax": 88000,
                                                                "netIncome": 1896400
                                                            }
                                                        ]
                                                    }
                                                    """),
                                    @ExampleObject(name = "사장님 홈 화면 조회", summary = "사장님 홈 화면 정보",
                                            value = """
                                                    {
                                                        "nowMonth": 9,
                                                        "totalSalary": 1186000,
                                                        "prevMonthSalaryDiff": 86000,
                                                        "todayRoutineCounts": 5,
                                                        "ownerMonthlyWorkplaceSummaryInfoList": [
                                                            {
                                                                "workplaceSummaryInfo": {
                                                                    "workplaceId": 1,
                                                                    "workplaceName": "GS25 역삼점",
                                                                    "isShared": true
                                                                },
                                                                "monthlyWorkerSummaryInfoList": [
                                                                    {
                                                                        "nickname": "성실알바최씨",
                                                                        "totalWorkMinutes": 5280,
                                                                        "grossIncome": 960000,
                                                                        "netIncome": 847200
                                                                    },
                                                                    {
                                                                        "nickname": "주말알바이씨",
                                                                        "totalWorkMinutes": 2640,
                                                                        "grossIncome": 380000,
                                                                        "netIncome": 338800
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "workplaceSummaryInfo": {
                                                                    "workplaceId": 3,
                                                                    "workplaceName": "홍콩반점 홍대입구역점",
                                                                    "isShared": true
                                                                },
                                                                "monthlyWorkerSummaryInfoList": [
                                                                    {
                                                                        "nickname": "미소알바강씨",
                                                                        "totalWorkMinutes": 4800,
                                                                        "grossIncome": 820000,
                                                                        "netIncome": 750000
                                                                    }
                                                                ]
                                                            }
                                                        ]
                                                    }
                                                    """)
                    })),
            @ApiResponse(responseCode = "403", description = "역할에 맞지 않는 접근", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    ResponseEntity<?> getTodayHomeInfo();
}
