package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "근무 생성 응답 DTO")
public class WorkCreateResponse {
    @Schema(description = "생성된 근무 ID 배열", example = "[\"1\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> workId;
}
