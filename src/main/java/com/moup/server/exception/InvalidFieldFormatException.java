package com.moup.server.exception;

public class InvalidFieldFormatException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_FIELD_FORMAT;
    }
}
