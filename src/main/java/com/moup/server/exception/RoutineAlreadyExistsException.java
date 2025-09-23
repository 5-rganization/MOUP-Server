package com.moup.server.exception;

public class RoutineAlreadyExistsException extends CustomException {
    public ErrorCode getErrorCode() {
        return ErrorCode.ROUTINE_ALREADY_EXISTS;
    }
}
