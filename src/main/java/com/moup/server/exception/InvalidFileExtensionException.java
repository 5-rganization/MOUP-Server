package com.moup.server.exception;

public class InvalidFileExtensionException extends CustomException {
    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_FILE_EXTENSION;
    }
}
