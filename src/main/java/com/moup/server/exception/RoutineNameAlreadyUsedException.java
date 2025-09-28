package com.moup.server.exception;

public class RoutineNameAlreadyUsedException extends CustomException {
    public ErrorCode getErrorCode() {
        return ErrorCode.ROUTINE_NAME_ALREADY_USED;
    }
}
