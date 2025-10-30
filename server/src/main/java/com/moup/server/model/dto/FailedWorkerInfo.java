package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "근무 생성 실패 근무자 정보 DTO")
public class FailedWorkerInfo {
    @Schema(description = "근무 생성에 실패한 근무자 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workerId;
    @Schema(description = "유저 닉네임", example = "moup1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
    @Schema(description = "실패 이유", example = "요청한 근무자(ID: 3)는 탈퇴했거나 존재하지 않는 근무자입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
