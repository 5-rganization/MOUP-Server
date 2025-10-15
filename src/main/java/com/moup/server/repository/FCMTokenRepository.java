package com.moup.server.repository;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
public interface FCMTokenRepository {

  @Update("UPDATE users SET fcm_token = #{fcmToken} WHERE id = #{userId}")
  void updateUserFCMToken(Long userId, String fcmToken);
}
