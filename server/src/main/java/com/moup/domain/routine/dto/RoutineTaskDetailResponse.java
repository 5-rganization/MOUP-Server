package com.moup.domain.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "할 일 상세 조회 응답 DTO")
public class RoutineTaskDetailResponse {
    @Schema(description = "할 일 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;
    @Schema(description = "루틴 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routineId;
    @Schema(description = "내용", example = "바닥 청소", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    @Schema(description = "정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderIndex;
    /// 근무 문맥이 있을 때만 채워진다. 완료는 (근무, 할 일) 쌍이므로
    /// 근무를 모르는 루틴 단독 조회에서는 의미가 없어 null이다.
    @Schema(description = "이 근무에서 완료했는지 (근무 문맥이 없는 조회에서는 null)",
            example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isCompleted;
}
