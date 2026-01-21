package com.moup.domain.salary.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class SalaryWorkerNotFoundException extends CustomException {
    public SalaryWorkerNotFoundException() {
        super(ErrorCode.SALARY_WORKER_NOT_FOUND);
    }

    public SalaryWorkerNotFoundException(String message) {
        super(ErrorCode.SALARY_WORKER_NOT_FOUND, message);
    }
}
