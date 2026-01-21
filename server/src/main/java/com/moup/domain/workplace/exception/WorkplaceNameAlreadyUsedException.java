package com.moup.domain.workplace.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class WorkplaceNameAlreadyUsedException extends CustomException {
    public WorkplaceNameAlreadyUsedException() {
        super(ErrorCode.WORKPLACE_NAME_ALREADY_USED);
    }

    public WorkplaceNameAlreadyUsedException(String message) {
        super(ErrorCode.WORKPLACE_NAME_ALREADY_USED, message);
    }

}
