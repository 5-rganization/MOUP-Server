package com.moup.server.exception;

public class WorkplaceNotFoundException extends CustomException {
    public WorkplaceNotFoundException() {
        super(ErrorCode.WORKPLACE_NOT_FOUND);
    }

    public WorkplaceNotFoundException(String message) {
        super(ErrorCode.WORKPLACE_NOT_FOUND, message);
    }
}
