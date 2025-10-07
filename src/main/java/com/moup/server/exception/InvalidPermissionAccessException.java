package com.moup.server.exception;

public class InvalidPermissionAccessException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_PERMISSION_ACCESS;
    }
}
