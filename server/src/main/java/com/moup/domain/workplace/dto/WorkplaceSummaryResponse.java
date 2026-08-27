package com.moup.domain.workplace.dto;

import com.moup.domain.workplace.domain.WorkplaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무지(매장) 요약 조회 응답 DTO")
public class WorkplaceSummaryResponse {
    @Schema(description = "근무지(매장) ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workplaceId;
    @Schema(description = "근무지(매장) 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @Schema(description = "공유 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isShared;
    /// 근무지 목록·단건 조회에서만 의미가 있다. 급여·근무 응답에 끼어 있는 요약에는
    /// 근무자 컨텍스트가 없으므로 기본값 `ACTIVE`가 그대로 나간다.
    @Schema(description = "근무지 상태. PENDING_APPROVAL이면 승인 대기 중이라 근무 등록·출퇴근이 차단된다.",
            example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    @Builder.Default
    private WorkplaceStatus status = WorkplaceStatus.ACTIVE;
}
