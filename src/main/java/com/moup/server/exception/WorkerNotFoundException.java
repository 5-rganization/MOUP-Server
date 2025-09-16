package com.moup.server.exception;

public class WorkerNotFoundException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKER_NOT_FOUND;
    }
}
