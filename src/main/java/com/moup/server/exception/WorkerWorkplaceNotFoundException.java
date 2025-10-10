package com.moup.server.exception;

public class WorkerWorkplaceNotFoundException extends CustomException {
    public WorkerWorkplaceNotFoundException() {
        super(ErrorCode.WORKER_WORKPLACE_NOT_FOUND);
    }

    public WorkerWorkplaceNotFoundException(String message) {
        super(ErrorCode.WORKER_WORKPLACE_NOT_FOUND, message);
    }
}
