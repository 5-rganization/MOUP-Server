package com.moup.server.exception;

public class CannotDeleteDataException extends CustomException {
    public CannotDeleteDataException() {
        super(ErrorCode.CANNOT_DELETE_DATA);
    }

    public CannotDeleteDataException(String message) {
        super(ErrorCode.CANNOT_DELETE_DATA, message);
    }
}
