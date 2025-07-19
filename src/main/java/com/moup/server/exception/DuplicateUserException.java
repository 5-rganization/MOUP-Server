package com.moup.server.exception;

public class DuplicateUserException extends CustomException {

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.USER_ALREADY_EXISTS;
    }
}
