package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonSubTypes({@JsonSubTypes.Type(WorkerHomeResponse.class), @JsonSubTypes.Type(OwnerHomeResponse.class)})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseHomeResponse {
    @Schema(description = "기준 달 (현재 월)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer nowMonth;
    @Schema(description = "기준 달 급여(인건비) 정보", example = "549000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalSalary;
    @Schema(description = "오늘의 루틴 개수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer todayRoutineCounts;
}
