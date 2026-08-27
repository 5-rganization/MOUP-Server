package com.moup.global.security.token;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface SocialTokenRepository {
    /// 탈퇴 확정 시 소셜 자격증명을 지운다.
    /// 하드 삭제를 없앴으므로 users CASCADE가 더 이상 발화하지 않는다.
    @Delete("DELETE FROM social_tokens WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);

    @Select("SELECT * FROM social_tokens WHERE user_id = #{userId}")
    Optional<SocialToken> findByUserId(Long userId);

    @Update("UPDATE social_tokens SET refresh_token = #{refreshToken}, updated_at = CURRENT_TIMESTAMP() WHERE id = #{id}")
    void updateById(Long id, String refreshToken);

    /// `UNIQUE (user_id)` + upsert. 사유는 `UserTokenRepository.save` 참조.
    @Insert("INSERT INTO social_tokens (user_id, refresh_token) VALUES (#{userId}, #{refreshToken}) "
            + "ON DUPLICATE KEY UPDATE refresh_token = VALUES(refresh_token), updated_at = CURRENT_TIMESTAMP()")
    void save(SocialToken socialToken);
}
