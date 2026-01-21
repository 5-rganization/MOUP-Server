package com.moup.domain.user.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkerNotFoundException extends CustomException {
    public WorkerNotFoundException() {
        super(ErrorCode.WORKER_NOT_FOUND);
    }

    public WorkerNotFoundException(String message) {
        super(ErrorCode.WORKER_NOT_FOUND, message);
    }
}
