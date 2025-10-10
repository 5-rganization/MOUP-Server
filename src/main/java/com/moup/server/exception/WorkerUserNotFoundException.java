package com.moup.server.exception;

public class WorkerUserNotFoundException extends CustomException {
    public WorkerUserNotFoundException() {
        super(ErrorCode.WORKER_USER_NOT_FOUND);
    }

    public WorkerUserNotFoundException(String message) {
        super(ErrorCode.WORKER_USER_NOT_FOUND, message);
    }
}
