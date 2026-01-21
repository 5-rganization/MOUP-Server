package com.moup.domain.user.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkerAlreadyExistsException extends CustomException {
    public WorkerAlreadyExistsException() {
        super(ErrorCode.WORKER_ALREADY_EXISTS);
    }

    public WorkerAlreadyExistsException(String message) {
        super(ErrorCode.WORKER_ALREADY_EXISTS, message);
    }
}
