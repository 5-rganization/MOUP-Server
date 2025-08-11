package com.moup.server.exception;

public class InvalidTokenException extends CustomException {
  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_TOKEN;
  }
}
