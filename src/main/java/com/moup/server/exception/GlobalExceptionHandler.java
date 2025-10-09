package com.moup.server.exception;

import com.moup.server.model.dto.ErrorResponse;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author neoskyclad 전역 예외 처리기
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException e) {
        return response(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
        logger.warn("Invalid field value provided");
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return response(ErrorCode.INVALID_FIELD_FORMAT, errorMessage);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("Invalid parameter type provided for '{}': value '{}'", e.getName(), e.getValue());
        return response(ErrorCode.INVALID_ARGUMENT);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("Invalid varable provided");
        String errorMessage = e.getMessage();
        return response(ErrorCode.INVALID_VARIABLE_FORMAT, errorMessage);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleException(RuntimeException e) {
        logger.error("Unhandled runtime exception", e);
        return e.getMessage().isEmpty() ? response(ErrorCode.INTERNAL_SERVER_ERROR)
                : response(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    ResponseEntity<?> response(ErrorCode errorCode) {
        ErrorResponse errorResponse = ErrorResponse.builder().errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage()).build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

    ResponseEntity<?> response(ErrorCode errorCode, String errorMessage) {
        ErrorResponse errorResponse = ErrorResponse.builder().errorCode(errorCode.getCode()).errorMessage(errorMessage)
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

}
