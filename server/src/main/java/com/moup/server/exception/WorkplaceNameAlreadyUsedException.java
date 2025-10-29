package com.moup.server.exception;

public class WorkplaceNameAlreadyUsedException extends CustomException {
    public WorkplaceNameAlreadyUsedException() {
        super(ErrorCode.WORKPLACE_NAME_ALREADY_USED);
    }

    public WorkplaceNameAlreadyUsedException(String message) {
        super(ErrorCode.WORKPLACE_NAME_ALREADY_USED, message);
    }

}
