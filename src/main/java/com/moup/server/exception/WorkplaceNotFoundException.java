package com.moup.server.exception;

public class WorkplaceNotFoundException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKPLACE_NOT_FOUND;
    }
}
