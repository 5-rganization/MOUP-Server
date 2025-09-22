package com.moup.server.exception;

public class SalaryNotFoundException extends CustomException {
    public ErrorCode getErrorCode() {
        return ErrorCode.SALARY_NOT_FOUND;
    }
}
