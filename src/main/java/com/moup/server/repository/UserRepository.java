package com.moup.server.repository;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.model.dto.UserCreateRequest;
import com.moup.server.model.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {

    /**
     * 사용자를 생성하고, 생성된 user_id를 반환.
     *
     * @param user
     * @return user_id
     */
    @Insert("INSERT INTO users (provider, provider_id, username) VALUES (#{provider}, #{providerId}, #{username})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(UserCreateRequest userCreateRequest);

    @Select("SELECT EXISTS(SELECT 1 FROM users WHERE id = #{id})")
    boolean existById(Long id);

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

    @Update("UPDATE users SET nickname = #{nickname} AND role = #{role} WHERE id = #{id}")
    void updateById(Long id, String nickname, Role role);

    @Update("UPDATE users SET nickname = #{nickname} WHERE id = #{id}")
    void updateNicknameById(Long userId, String nickname);
}
