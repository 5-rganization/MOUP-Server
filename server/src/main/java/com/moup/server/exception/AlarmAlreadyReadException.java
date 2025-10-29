package com.moup.server.exception;

public class AlarmAlreadyReadException extends CustomException {

  public AlarmAlreadyReadException() {
    super(ErrorCode.ALARM_ALREADY_READ);
  }

  public AlarmAlreadyReadException(String message) {
    super(ErrorCode.ALARM_ALREADY_READ, message);
  }
}
