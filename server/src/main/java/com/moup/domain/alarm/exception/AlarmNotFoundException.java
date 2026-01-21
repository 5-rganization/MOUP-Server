package com.moup.domain.alarm.exception;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class AlarmNotFoundException extends CustomException {

  public AlarmNotFoundException() {
    super(ErrorCode.ALARM_NOT_FOUND);
  }

  public AlarmNotFoundException(String message) {
    super(ErrorCode.ALARM_NOT_FOUND, message);
  }
}
