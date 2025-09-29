package com.moup.server.exception;

public class RoutineNotFoundException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.ROUTINE_NOT_FOUND;
    }
}
