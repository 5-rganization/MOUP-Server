package com.moup.server.exception;

public class WorkerAlreadyExistsException extends CustomException {
    public WorkerAlreadyExistsException() {
        super(ErrorCode.WORKER_ALREADY_EXISTS);
    }

    public WorkerAlreadyExistsException(String message) {
        super(ErrorCode.WORKER_ALREADY_EXISTS, message);
    }
}
