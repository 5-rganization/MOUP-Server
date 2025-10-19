package com.moup.server.exception;

public class WorkNotFoundException extends CustomException {
    public WorkNotFoundException() {
        super(ErrorCode.WORK_NOT_FOUND);
    }

    public WorkNotFoundException(String message) {
        super(ErrorCode.WORK_NOT_FOUND, message);
    }
}
