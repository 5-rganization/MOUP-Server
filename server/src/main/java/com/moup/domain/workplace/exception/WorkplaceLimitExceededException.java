package com.moup.domain.workplace.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkplaceLimitExceededException extends CustomException {

    public WorkplaceLimitExceededException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public WorkplaceLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }
}
