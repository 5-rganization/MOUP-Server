package com.moup.server.repository;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {

    @Insert("INSERT INTO users (provider, provider_id, username, nickname, role) VALUES (#{provider}, #{providerId}, #{username}, #{nickname}, #{role})")
    void create(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    @Select("SELECT * FROM users WHERE provider = #{provider} AND provider_id = #{providerId}")
    Optional<User> findByProviderAndId(Login provider, String providerId);

    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    @Update("UPDATE users SET profile_img = #{profileImg} WHERE id = #{id}")
    void updateProfileImg(Long id, String profileImg);

    @Update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP(), is_deleted = 1 WHERE id = #{id}")
    void softDeleteUserById(Long id);

    @Update("UPDATE users SET deleted_at = null, is_deleted = 0 WHERE id = #{id}")
    void undeleteUserById(Long id);

    @Delete("DELETE FROM users WHERE id = #{id}")
    void hardDeleteUserById(Long id);

    @Select("SELECT * FROM users WHERE is_deleted = 1 AND deleted_at < #{threeDaysAgo}")
    List<User> findAllHardDeleteUsers(LocalDateTime threeDaysAgo);
}
