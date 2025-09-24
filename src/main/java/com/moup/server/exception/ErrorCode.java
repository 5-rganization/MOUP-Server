package com.moup.server.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "FILE_400", "잘못된 파일 형식입니다."),
    INVALID_FIELD_FORMAT(HttpStatus.UNPROCESSABLE_ENTITY, "FIELD_422", "유효하지 않은 형식입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_400", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증되지 않은 사용자입니다."),
    INVALID_ROLE_ACCESS(HttpStatus.FORBIDDEN, "ROLE_403", "역할에 맞지 않는 접근입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "해당 유저는 존재하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 등록된 유저입니다."),
    WORKPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKPLACE_404", "해당 근무지(매장)는 존재하지 않습니다."),
    WORKPLACE_NAME_ALREADY_USED(HttpStatus.CONFLICT, "WORKPLACE_NAME_409", "사용자가 이미 등록한 근무지 이름입니다."),
    WORKER_WORKPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER_404", "요청한 근무지에 해당하는 근무자가 존재하지 않습니다."),
    WORKER_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER_404", "사용자에 해당하는 근무지가 존재하지 않습니다."),
    WORKER_ALREADY_EXISTS(HttpStatus.CONFLICT, "WORKER_409", "이미 등록된 근무자입니다."),
    SALARY_WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "SALARY_404", "근무자에 해당하는 급여가 존재하지 않습니다."),
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTINE_404", "해당 루틴은 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버에 오류가 발생했습니다."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "DELETE_409", "삭제 처리 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
