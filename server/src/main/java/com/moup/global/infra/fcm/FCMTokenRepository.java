package com.moup.global.infra.fcm;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FCMTokenRepository {

  @Update("UPDATE users SET fcm_token = #{fcmToken} WHERE id = #{userId}")
  void updateUserFCMToken(Long userId, String fcmToken);

  @Update("UPDATE users SET fcm_token = null WHERE id = #{userId}")
  void deleteFCMToken(Long userId);
}
