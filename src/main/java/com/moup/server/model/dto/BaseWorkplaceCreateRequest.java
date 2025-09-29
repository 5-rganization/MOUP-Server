package com.moup.server.model.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(WorkerWorkplaceCreateRequest.class), @JsonSubTypes.Type(OwnerWorkplaceCreateRequest.class)})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseWorkplaceCreateRequest {
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "근무지/매장 이름", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workplaceName;
    @NotBlank(message = "빈 값 혹은 공백 문자는 받을 수 없습니다.")
    @Schema(description = "근무지/매장 카테고리 이름", example = "편의점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "주소", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @Schema(description = "위도", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @Schema(description = "경도", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;

    public abstract Workplace toWorkplaceEntity(Long userId);
    public abstract Worker toWorkerEntity(Long userId, Long workplaceId);
}
