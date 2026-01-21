package com.moup.domain.work.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkNotFoundException extends CustomException {
    public WorkNotFoundException() {
        super(ErrorCode.WORK_NOT_FOUND);
    }

    public WorkNotFoundException(String message) {
        super(ErrorCode.WORK_NOT_FOUND, message);
    }
}
