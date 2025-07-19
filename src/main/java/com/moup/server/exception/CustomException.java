package com.moup.server.exception;

import lombok.Getter;

public abstract class CustomException extends RuntimeException {
    public abstract ErrorCode getErrorCode();
}
