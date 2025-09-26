package com.moup.server.exception;

public class InvalidRoleAccessException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_ROLE_ACCESS;
    }
}
