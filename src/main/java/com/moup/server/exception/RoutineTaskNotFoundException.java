package com.moup.server.exception;

public class RoutineTaskNotFoundException extends CustomException {
    public ErrorCode getErrorCode() {
        return ErrorCode.ROUTINETASK_NOT_FOUND;
    }
}
