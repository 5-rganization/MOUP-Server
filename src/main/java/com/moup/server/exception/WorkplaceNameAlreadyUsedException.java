package com.moup.server.exception;

public class WorkplaceNameAlreadyUsedException extends CustomException {
    @Override
    public ErrorCode getErrorCode() { return ErrorCode.WORKPLACE_NAME_ALREADY_USED; }

}
