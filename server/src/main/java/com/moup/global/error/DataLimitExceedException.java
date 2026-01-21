package com.moup.global.error;

public class DataLimitExceedException extends CustomException {
    public DataLimitExceedException() {
        super(ErrorCode.DATA_LIMIT_EXCEED);
    }

    public DataLimitExceedException(String message) {
        super(ErrorCode.DATA_LIMIT_EXCEED, message);
    }
}
