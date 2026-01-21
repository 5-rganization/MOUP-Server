package com.moup.domain.user.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class UserAlreadyExistsException extends CustomException {
    public UserAlreadyExistsException() {
        super(ErrorCode.USER_ALREADY_EXISTS);
    }

    public UserAlreadyExistsException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }
}
