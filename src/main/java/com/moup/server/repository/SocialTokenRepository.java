package com.moup.server.repository;

import com.moup.server.model.entity.SocialToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface SocialTokenRepository {
    @Select("SELECT * FROM social_tokens WHERE user_id = #{userId}")
    Optional<SocialToken> findByUserId(Long userId);

    @Update("UPDATE social_tokens SET access_token = #{accessToken}, refresh_token = #{refreshToken}, updated_at = CURRENT_TIMESTAMP() WHERE id = #{id}")
    void updateById(Long id, String accessToken, String refreshToken);

    @Insert("INSERT INTO social_tokens (user_id, access_token, refresh_token) VALUES (#{userId}, #{accessToken}, #{refreshToken})")
    void save(SocialToken socialToken);
}
