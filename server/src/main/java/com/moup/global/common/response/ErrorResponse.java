package com.moup.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * @author neoskyclad
 * 
 * - 이슈 사항: 이미 security.web에 동일한 이름의 interface 존재.
 * - 만약 import한 라이브러리에 동일한 이름있다면 그대로 사용할 것
 */
@Getter
@Builder
@Schema(description = "에러 응답 DTO")
public class ErrorResponse {
    @Schema(description = "에러 코드", example = "ERROR_400")
    private String errorCode;
    @Schema(description = "에러 메시지", example = "에러 메시지 입니다.")
    private String errorMessage;
}
