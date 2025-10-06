package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "초대 코드 생성 응답 DTO")
public class InviteCodeGenerateResponse {
    @Schema(description = "생성된 초대 코드", example = "MUP234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
}
