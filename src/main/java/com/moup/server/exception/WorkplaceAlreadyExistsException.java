package com.moup.server.exception;

public class WorkplaceAlreadyExistsException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.WORKPLACE_ALREADY_EXISTS;
    }
}
