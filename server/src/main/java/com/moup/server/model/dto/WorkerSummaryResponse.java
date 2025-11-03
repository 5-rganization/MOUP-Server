package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무자 요약 조회 응답 DTO")
public class WorkerSummaryResponse {
    @Schema(description = "근무자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
    @Schema(description = "라벨 색상 (알바생 기준)", example = "RED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String workerBasedLabelColor;
    @Schema(description = "라벨 색상 (사장님 기준)", example = "BLUE", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String ownerBasedLabelColor;
    @Schema(description = "사용자 닉네임", example = "김사장", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
    @Schema(description = "사용자 프로필 이미지", example = "https://moup-bucket.s3.ap-northeast-2.amazonaws.com/5aedbc811d19b48d5151c9d05b48fc6751be282f5e89f478a3b81dbc16e2ada7.png", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String profileImg;
}
