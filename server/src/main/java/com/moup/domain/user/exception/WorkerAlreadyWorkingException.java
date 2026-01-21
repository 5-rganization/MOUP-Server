package com.moup.domain.user.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkerAlreadyWorkingException extends CustomException {
    public WorkerAlreadyWorkingException() {
        super(ErrorCode.WORKER_ALREADY_WORKING);
    }

    public WorkerAlreadyWorkingException(String message) {
        super(ErrorCode.WORKER_ALREADY_WORKING, message);
    }
}
