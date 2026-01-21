package com.moup.domain.routine.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class RoutineNameAlreadyUsedException extends CustomException {
    public RoutineNameAlreadyUsedException() {
        super(ErrorCode.ROUTINE_NAME_ALREADY_USED);
    }

    public RoutineNameAlreadyUsedException(String message) {
        super(ErrorCode.ROUTINE_NAME_ALREADY_USED, message);
    }
}
