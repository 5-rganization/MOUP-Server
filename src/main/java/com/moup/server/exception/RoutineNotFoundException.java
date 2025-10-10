package com.moup.server.exception;

public class RoutineNotFoundException extends CustomException {
    public RoutineNotFoundException() {
        super(ErrorCode.ROUTINE_NOT_FOUND);
    }

    public RoutineNotFoundException(String message) {
        super(ErrorCode.ROUTINE_NOT_FOUND, message);
    }
}
