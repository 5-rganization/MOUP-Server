package com.moup.server.exception;

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

    /**
     * 커스텀 런타임 에러
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException e) {
        return response(e.getErrorCode());
    }

    /**
     * 일반 런타임 에러
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleException(RuntimeException e) {
        logger.error("Unhandled runtime exception", e);
        return response(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 공통 응답 포맷
     *
     * @param errorCode
     * @return
     */
    ResponseEntity<?> response(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Map.of("errorCode", errorCode.getCode(), "errorMessage", errorCode.getMessage()));
    }

}
