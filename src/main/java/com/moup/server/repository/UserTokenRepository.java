package com.moup.server.repository;


import com.moup.server.model.entity.UserToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
public interface UserTokenRepository {

    @Select("SELECT * FROM user_tokens WHERE user_id = #{userId}")
    Optional<UserToken> findByUserId(Long userId);

    @Update("UPDATE user_tokens SET refresh_token = #{refreshToken}, expiry_date = #{expiryDate}, created_at = CURRENT_TIMESTAMP() WHERE id = #{id}")
    void updateById(Long id, String refreshToken, String expiryDate);

    @Insert("INSERT INTO user_tokens (user_id, refresh_token, expiry_date) VALUES (#{userId}, #{refreshToken}, #{expiryDate})")
    void save(UserToken userToken);
}
