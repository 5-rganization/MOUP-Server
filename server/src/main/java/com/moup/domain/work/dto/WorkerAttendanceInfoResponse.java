package com.moup.domain.work.dto;

import com.moup.domain.salary.dto.SalaryDetailResponse;
import com.moup.domain.user.dto.WorkerWorkAttendanceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "알바생 근태 정보 응답 DTO")
public class WorkerAttendanceInfoResponse {
    @Schema(description = "근무지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "근무자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
    @Schema(description = "알바생 출석 정보 (없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkerWorkAttendanceResponse> workerWorkAttendanceInfoList;
    @Schema(description = "알바생의 급여 설정. 사장님이 급여를 부분 수정하기 전에 현재 값을 확인하는 용도. 급여 정보가 없으면 null", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private SalaryDetailResponse salaryDetailInfo;
}
