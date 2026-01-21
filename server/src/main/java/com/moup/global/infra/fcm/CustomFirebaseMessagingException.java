package com.moup.global.infra.fcm;

import com.moup.global.error.CustomException;
import com.moup.global.error.ErrorCode;

public class CustomFirebaseMessagingException extends CustomException {

  public CustomFirebaseMessagingException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
