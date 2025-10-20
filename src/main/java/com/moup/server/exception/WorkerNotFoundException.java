package com.moup.server.exception;

public class WorkerNotFoundException extends CustomException {
    public WorkerNotFoundException() {
        super(ErrorCode.WORKER_NOT_FOUND);
    }

    public WorkerNotFoundException(String message) {
        super(ErrorCode.WORKER_NOT_FOUND, message);
    }
}
