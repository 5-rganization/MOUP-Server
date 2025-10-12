package com.moup.server.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.moup.server.model.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
