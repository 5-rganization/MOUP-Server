package com.moup.domain.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "근무별 할 일 체크 요청 DTO")
public class RoutineTaskCompletionRequest {
    /// 체크/해제를 한 엔드포인트로 받는다. 토글이 아니라 **원하는 상태를 그대로 보내는**
    /// 방식이라 멱등하다 — 재시도해도 화면과 서버가 어긋나지 않는다.
    @NotNull(message = "필수 입력값입니다.")
    @Schema(description = "true면 완료, false면 완료 해제", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean completed;
}
