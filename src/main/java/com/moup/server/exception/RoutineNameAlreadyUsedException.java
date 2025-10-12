package com.moup.server.exception;

public class RoutineNameAlreadyUsedException extends CustomException {
    public RoutineNameAlreadyUsedException() {
        super(ErrorCode.ROUTINE_NAME_ALREADY_USED);
    }

    public RoutineNameAlreadyUsedException(String message) {
        super(ErrorCode.ROUTINE_NAME_ALREADY_USED, message);
    }
}
