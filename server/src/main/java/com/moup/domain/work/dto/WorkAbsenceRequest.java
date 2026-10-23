package com.moup.domain.work.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "근무 결근 설정 요청 DTO")
public class WorkAbsenceRequest {
    /// 토글이 아니라 **원하는 상태를 그대로 보내는** 방식이라 멱등하다.
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "true면 결근 처리, false면 결근 해제", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isAbsent;
}
