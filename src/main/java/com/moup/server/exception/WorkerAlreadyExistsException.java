package com.moup.server.exception;

public class WorkerAlreadyExistsException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKER_ALREADY_EXISTS;
    }
}
