package com.moup.server.exception;

public class WorkplaceLimitExceededException extends CustomException {

    public WorkplaceLimitExceededException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public WorkplaceLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }
}
