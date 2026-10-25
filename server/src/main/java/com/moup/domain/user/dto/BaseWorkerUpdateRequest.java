package com.moup.domain.user.dto;

import com.moup.domain.salary.dto.SalaryUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/// 근무자 수정 공통부. 두 하위 타입은 컨트롤러가 각자 구체 타입으로 받으므로 다형 역직렬화가 필요 없다.
/// 예전에 붙어 있던 `@JsonTypeInfo(DEDUCTION)`은 판별 키(라벨색)를 생략한 부분 수정 요청을 거부하게 만들 뿐이라 뺐다.
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseWorkerUpdateRequest {
    @Valid
    @Schema(description = "급여 정보 — 생략하면 급여 설정을 바꾸지 않는다. 보낸 필드만 반영된다", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private SalaryUpdateRequest salaryUpdateRequest;
}
