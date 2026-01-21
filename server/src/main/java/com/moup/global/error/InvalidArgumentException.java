package com.moup.global.error;

public class InvalidArgumentException extends CustomException {
    public InvalidArgumentException() {
        super(ErrorCode.INVALID_ARGUMENT);
    }

    public InvalidArgumentException(String message) {
        super(ErrorCode.INVALID_ARGUMENT, message);
    }
}
