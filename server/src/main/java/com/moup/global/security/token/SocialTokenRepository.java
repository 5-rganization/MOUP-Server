package com.moup.global.security.token;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface SocialTokenRepository {
    @Select("SELECT * FROM social_tokens WHERE user_id = #{userId}")
    Optional<SocialToken> findByUserId(Long userId);

    @Update("UPDATE social_tokens SET refresh_token = #{refreshToken}, updated_at = CURRENT_TIMESTAMP() WHERE id = #{id}")
    void updateById(Long id, String refreshToken);

    @Insert("INSERT INTO social_tokens (user_id, refresh_token) VALUES (#{userId}, #{refreshToken})")
    void save(SocialToken socialToken);
}
