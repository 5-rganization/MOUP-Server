package com.moup.server.exception;

public class WorkerAlreadyWorkingException extends CustomException {
    public WorkerAlreadyWorkingException() {
        super(ErrorCode.WORKER_ALREADY_WORKING);
    }

    public WorkerAlreadyWorkingException(String message) {
        super(ErrorCode.WORKER_ALREADY_WORKING, message);
    }
}
