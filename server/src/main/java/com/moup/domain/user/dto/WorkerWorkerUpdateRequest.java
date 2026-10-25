package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@Schema(description = "알바생 근무자 업데이트 요청 DTO")
public class WorkerWorkerUpdateRequest extends BaseWorkerUpdateRequest {
    // PATCH이므로 생략은 "건드리지 않음"이다. 보냈다면 공백이면 안 된다. @Pattern·@Size는 null을 통과시킨다.
    @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Size(max = 10, message = "10자 이내로 입력해야 합니다.")
    @Schema(description = "라벨 색상 (알바생 기준) — 생략 시 기존 값 유지", example = "RED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String workerBasedLabelColor;
}
