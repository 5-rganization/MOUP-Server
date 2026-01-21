package com.moup.global.error;

public class InvalidFieldFormatException extends CustomException {
    public InvalidFieldFormatException() {
        super(ErrorCode.INVALID_FIELD_FORMAT);
    }

    public InvalidFieldFormatException(String message) {
        super(ErrorCode.INVALID_FIELD_FORMAT, message);
    }
}
