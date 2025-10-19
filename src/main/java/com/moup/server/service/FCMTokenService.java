package com.moup.server.service;

import com.moup.server.model.entity.User;
import com.moup.server.repository.FCMTokenRepository;
import com.moup.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

  private final FCMTokenRepository fcmTokenRepository;
  private final UserRepository userRepository;

  public void updateUserFCMToken(Long userId, String fcmToken) {
    fcmTokenRepository.updateUserFCMToken(userId, fcmToken);
  }

  public void deleteUserFCMToken(Long userId) {
    fcmTokenRepository.deleteFCMToken(userId);
  }
}
