package com.moup.domain.alarm.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class AlarmAlreadyReadException extends CustomException {

  public AlarmAlreadyReadException() {
    super(ErrorCode.ALARM_ALREADY_READ);
  }

  public AlarmAlreadyReadException(String message) {
    super(ErrorCode.ALARM_ALREADY_READ, message);
  }
}
