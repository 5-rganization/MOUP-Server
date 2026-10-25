package com.moup.domain.user.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

/// 승인·거절·내보내기가 근무자의 현재 상태와 맞지 않는다 (예: 이미 승인된 근무자 거절).
public class WorkerStateConflictException extends CustomException {
    public WorkerStateConflictException(String message) {
        super(ErrorCode.WORKER_STATE_CONFLICT, message);
    }
}
