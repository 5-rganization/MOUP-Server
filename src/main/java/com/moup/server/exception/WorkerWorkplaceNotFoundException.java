package com.moup.server.exception;

public class WorkerWorkplaceNotFoundException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKER_WORKPLACE_NOT_FOUND;
    }
}
