package com.moup.global.infra.fcm;

import com.moup.domain.user.mapper.UserRepository;
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
