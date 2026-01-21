package com.moup.domain.routine.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class RoutineNotFoundException extends CustomException {
    public RoutineNotFoundException() {
        super(ErrorCode.ROUTINE_NOT_FOUND);
    }

    public RoutineNotFoundException(String message) {
        super(ErrorCode.ROUTINE_NOT_FOUND, message);
    }
}
