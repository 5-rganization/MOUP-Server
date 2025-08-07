package com.moup.server.exception;

public class AlreadyDeletedException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.ALREADY_DELETED;
    }
}
