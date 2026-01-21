package com.moup.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.moup.domain.salary.dto.SalaryUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(WorkerWorkerUpdateRequest.class), @JsonSubTypes.Type(OwnerWorkerUpdateRequest.class)})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseWorkerUpdateRequest {
    @Valid
    @Schema(description = "급여 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryUpdateRequest salaryUpdateRequest;
}
