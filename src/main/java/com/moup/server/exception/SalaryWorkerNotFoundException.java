package com.moup.server.exception;

public class SalaryWorkerNotFoundException extends CustomException {
    public ErrorCode getErrorCode() {
        return ErrorCode.SALARY_WORKER_NOT_FOUND;
    }
}
