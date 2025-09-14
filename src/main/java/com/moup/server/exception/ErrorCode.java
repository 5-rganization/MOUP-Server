package com.moup.server.exception;

import com.amazonaws.services.cloudformation.model.AlreadyExistsException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "FILE_400", "잘못된 파일 형식입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_400", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증되지 않은 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "해당 유저는 존재하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 등록된 유저입니다."),
    WORKPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKPLACE_404", "해당 근무지는 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버에 오류가 발생했습니다."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "DELETE_409", "삭제 처리 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
