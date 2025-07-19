package com.moup.server.exception;

public class UserNotFoundException extends CustomException {

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.USER_NOT_FOUND;
    }
}
