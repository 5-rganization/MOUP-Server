package com.moup.domain.user.mapper;

import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.dto.UserCreateRequest;
import com.moup.global.common.type.Role;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {
    @Insert("INSERT INTO users (provider, provider_id, username) VALUES (#{provider}, #{providerId}, #{username})")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "id")
    Long create(UserCreateRequest userCreateRequest);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    /// 여러 사용자 ID에 해당하는 모든 사용자를 조회하는 메서드
    ///
    /// @param idList 조회할 근무지 ID 리스트
    /// @return 조회된 Workplace 객체 리스트
    @Select("""
            <script>
                SELECT * FROM users
                WHERE id IN
                <foreach item="id" collection="idList" open="(" separator="," close=")">
                      #{id}
                </foreach>
            </script>
            """)
    List<User> findAllByIdListIn(@Param("idList") List<Long> idList);

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

    @Select("SELECT * FROM users WHERE is_deleted = 1")
    List<User> findAllHardDeleteUsers();

    @Select("SELECT * FROM users WHERE is_deleted = 1 AND deleted_at < #{threeDaysAgo}")
    List<User> findAllOldHardDeleteUsers(LocalDateTime threeDaysAgo);

    @Update("UPDATE users SET nickname = #{nickname}, role = #{role} WHERE id = #{id}")
    void updateById(Long id, String nickname, Role role);

    @Update("UPDATE users SET nickname = #{nickname} WHERE id = #{id}")
    void updateNicknameById(Long id, String nickname);

    @Select("SELECT * FROM users ORDER BY id LIMIT #{batchSize} OFFSET #{offset}")
    List<User> findUsersWithPaging(@Param("offset") int page, @Param("batchSize") int batchSize);
}
