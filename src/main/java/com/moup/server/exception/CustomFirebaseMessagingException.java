package com.moup.server.exception;

public class CustomFirebaseMessagingException extends CustomException {

  public CustomFirebaseMessagingException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
