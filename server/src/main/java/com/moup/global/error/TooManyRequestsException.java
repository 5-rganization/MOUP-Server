package com.moup.global.error;

public class TooManyRequestsException extends CustomException {
    public TooManyRequestsException() {
        super(ErrorCode.TOO_MANY_REQUESTS);
    }

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS, message);
    }
}
