package com.moup.server.exception;

public class InvalidDateTimeFormatException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_DATETIME_FORMAT;
    }
}
