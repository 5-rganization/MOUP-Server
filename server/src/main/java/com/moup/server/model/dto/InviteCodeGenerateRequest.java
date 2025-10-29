package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "초대 코드 생성 요청 DTO")
public class InviteCodeGenerateRequest {
    @Builder.Default
    @Schema(description = "초대 코드 강제 재생성 여부", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private boolean forceGenerate = false;
}
