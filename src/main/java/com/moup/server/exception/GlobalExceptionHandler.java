package com.moup.server.exception;

import com.moup.server.model.dto.ErrorResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleException(RuntimeException e) {
        logger.error("Unhandled runtime exception", e);
        return response(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    ResponseEntity<?> response(ErrorCode errorCode) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(errorResponse);
    }

}
