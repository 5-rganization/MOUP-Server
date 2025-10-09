package com.moup.server.exception;

public class InvalidArgumentException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_ARGUMENT;
    }
}
