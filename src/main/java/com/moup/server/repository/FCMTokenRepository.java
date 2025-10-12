package com.moup.server.repository;

import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface FCMTokenRepository {

  @Update("UPDATE users SET fcm_token = #{fcmToken} WHERE id = #{userId}")
  void updateUserFCMToken(Long userId, String fcmToken);

}
