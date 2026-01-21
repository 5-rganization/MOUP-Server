package com.moup.global.error;

public class AlreadyDeletedException extends CustomException {
    public AlreadyDeletedException() {
        super(ErrorCode.ALREADY_DELETED);
    }

    public AlreadyDeletedException(String message) {
        super(ErrorCode.ALREADY_DELETED, message);
    }
}
