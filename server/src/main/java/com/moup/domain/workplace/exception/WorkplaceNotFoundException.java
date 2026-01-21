package com.moup.domain.workplace.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkplaceNotFoundException extends CustomException {
    public WorkplaceNotFoundException() {
        super(ErrorCode.WORKPLACE_NOT_FOUND);
    }

    public WorkplaceNotFoundException(String message) {
        super(ErrorCode.WORKPLACE_NOT_FOUND, message);
    }
}
