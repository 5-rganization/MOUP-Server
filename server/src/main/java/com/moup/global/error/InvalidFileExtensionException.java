package com.moup.global.error;

public class InvalidFileExtensionException extends CustomException {
    public InvalidFileExtensionException() {
        super(ErrorCode.INVALID_FILE_EXTENSION);
    }

    public InvalidFileExtensionException(String message) {
        super(ErrorCode.INVALID_FILE_EXTENSION, message);
    }
}
