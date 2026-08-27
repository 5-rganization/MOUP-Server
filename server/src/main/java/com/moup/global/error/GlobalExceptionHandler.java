package com.moup.global.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.moup.global.common.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.security.auth.message.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author neoskyclad 전역 예외 처리기
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        logger.warn("Custom exception occurred", e);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode().getCode())
                .errorMessage(e.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.warn("Invalid HTTP request body", e);
        ErrorCode errorCode;
        String errorMessage;
        if (e.getCause() instanceof InvalidFormatException cause) {
            // 원인 예외가 InvalidFormatException인 경우 (Enum, 날짜 형식 등 내용 오류) -> 422
            errorCode = ErrorCode.INVALID_FIELD_FORMAT;
            String fieldName = cause.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            String allowedValues = "알 수 없음";
            if (cause.getTargetType() != null && cause.getTargetType().isEnum()) {
                allowedValues = Arrays.toString(cause.getTargetType().getEnumConstants());
            }
            errorMessage = String.format(
                    "'%s' 필드에 허용되지 않는 값('%s')이 입력되었습니다. (허용된 값: %s)",
                    fieldName, cause.getValue(), allowedValues
            );
        }  else {
            // 그 외 대부분의 경우 (JSON 문법 오류 등 구조적 문제) -> 400
            errorCode = ErrorCode.BAD_REQUEST;
            errorMessage = errorCode.getMessage();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorMessage)
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
        logger.warn("Invalid field value provided", e);
        ErrorCode errorCode = ErrorCode.INVALID_FIELD_FORMAT;
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorMessage)
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("Invalid parameter type provided", e);
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;
        String errorMessage = String.format("'%s' 항목에 잘못된 타입을 입력했습니다.", e.getName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorMessage)
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("Invalid variable provided", e);
        ErrorCode errorCode = ErrorCode.INVALID_VARIABLE_FORMAT;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(e.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    /// `@PreAuthorize` 등 메서드 보안의 인가 거부를 403으로 변환한다.
    ///
    /// `AuthorizationDeniedException`은 `AccessDeniedException`(→ `RuntimeException`) 하위이고
    /// 컨트롤러 프록시 안에서 발생하므로, 이 핸들러가 없으면 아래 catch-all이 먼저 잡아
    /// **500을 반환하고 정상 종료한다.** 그러면 `SecurityConfig`에 등록한 `accessDeniedHandler`가
    /// 있는 `ExceptionTranslationFilter`는 예외를 보지도 못한다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        logger.warn("Access denied: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_PERMISSION_ACCESS;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    /// checked 예외는 `RuntimeException` 하위가 아니라 아래 catch-all에 걸리지 않는다.
    /// 핸들러가 없으면 스프링 부트 기본 `/error` 응답으로 나가 `ErrorResponse` 형태가
    /// 아니게 되고, 클라이언트의 에러 파싱이 깨진다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleCheckedException(Exception e) {
        logger.error("Unhandled checked exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    /// 업로드 용량 초과는 500이 아니라 413이다.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        logger.warn("Upload size exceeded: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("FILE_413")
                .errorMessage("업로드 가능한 파일 크기를 초과했습니다.")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /// 소셜 인증 실패는 서버 오류가 아니라 401이다.
    /// 예전에는 `AuthController.login`이 `throws AuthException`(checked)인데 핸들러가 없어
    /// **인증 실패가 500으로 나갔다.**
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException e) {
        logger.warn("Social authentication failed: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleException(RuntimeException e) {
        logger.error("Unhandled runtime exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }
}
