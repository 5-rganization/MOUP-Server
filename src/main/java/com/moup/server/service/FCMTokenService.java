package com.moup.server.service;

import com.moup.server.model.entity.User;
import com.moup.server.repository.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

  private final FCMTokenRepository fcmTokenRepository;

  public void updateUserFCMToken(Long userId, String fcmToken) {
    fcmTokenRepository.updateUserFCMToken(userId, fcmToken);
  }
}
