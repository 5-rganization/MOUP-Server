package com.moup.server.exception;

public class WorkerUserNotFoundException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKER_USER_NOT_FOUND;
    }
}
