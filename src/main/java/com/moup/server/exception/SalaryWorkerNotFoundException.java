package com.moup.server.exception;

public class SalaryWorkerNotFoundException extends CustomException {
    public SalaryWorkerNotFoundException() {
        super(ErrorCode.SALARY_WORKER_NOT_FOUND);
    }

    public SalaryWorkerNotFoundException(String message) {
        super(ErrorCode.SALARY_WORKER_NOT_FOUND, message);
    }
}
