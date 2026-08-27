package com.moup.domain.workplace.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.moup.domain.user.dto.OwnerWorkplaceUpdateRequest;
import com.moup.domain.user.dto.WorkerWorkplaceUpdateRequest;
import com.moup.domain.workplace.domain.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(OwnerWorkplaceUpdateRequest.class), @JsonSubTypes.Type(WorkerWorkplaceUpdateRequest.class)})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseWorkplaceUpdateRequest {
    // 생성 DTO에는 검증이 있는데 수정 DTO에는 하나도 없었다. 스키마 제약을 넘기면
    // 422가 아니라 DB 오류로 500이 나간다.
    //
    // PATCH이므로 필드 생략은 "건드리지 않음"이다. 따라서 @NotBlank(= null도 거부)가
    // 아니라 "값을 보냈다면 공백이면 안 된다"로 검증한다. @Pattern은 null을 통과시킨다.
    @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Size(max = 50, message = "50자 이내로 입력해야 합니다.")
    @Schema(description = "근무지(매장) 이름 — 생략 시 기존 값 유지", example = "세븐일레븐 동탄중심상가점", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String workplaceName;
    @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "빈 값이나 공백 문자는 받을 수 없습니다.")
    @Size(max = 10, message = "10자 이내로 입력해야 합니다.")
    @Schema(description = "근무지(매장) 카테고리 이름 — 생략 시 기존 값 유지", example = "편의점", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String categoryName;
    @Size(max = 100, message = "100자 이내로 입력해야 합니다.")
    @Schema(description = "주소 — 생략 시 기존 값 유지", example = "경기 화성시 동탄중심상가1길 8 1층", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;
    @DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 범위여야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 -90 ~ 90 범위여야 합니다.")
    @Schema(description = "위도 — 생략 시 기존 값 유지", example = "37.2000891334382", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double latitude;
    @DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 범위여야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 -180 ~ 180 범위여야 합니다.")
    @Schema(description = "경도 — 생략 시 기존 값 유지", example = "127.072006099274", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double longitude;
    public abstract Workplace toWorkplaceEntity(Long workplaceId, Long ownerId);
}
