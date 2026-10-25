package com.moup.domain.workplace.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

/// 쓰지 않은 초대 코드를 정해진 횟수 안에 뽑지 못했다. 일시적인 상태라 다시 시도하면 된다.
public class InviteCodeGenerationException extends CustomException {
    public InviteCodeGenerationException() {
        super(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }
}
