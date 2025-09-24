package com.moup.server.exception;

public class InvalidParameterException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_PARAMETER;
    }
}
