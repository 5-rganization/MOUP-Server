package com.moup.server.exception;

public class AlarmNotFoundException extends CustomException {

  public AlarmNotFoundException() {
    super(ErrorCode.ALARM_NOT_FOUND);
  }

  public AlarmNotFoundException(String message) {
    super(ErrorCode.ALARM_NOT_FOUND, message);
  }
}
