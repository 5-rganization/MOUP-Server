package com.moup.global.error;

public class InvalidPermissionAccessException extends CustomException {
    public InvalidPermissionAccessException() {
        super(ErrorCode.INVALID_PERMISSION_ACCESS);
    }

    public InvalidPermissionAccessException(String message) {
        super(ErrorCode.INVALID_PERMISSION_ACCESS, message);
    }
}
