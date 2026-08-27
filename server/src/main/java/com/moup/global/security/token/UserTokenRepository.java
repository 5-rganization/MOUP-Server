package com.moup.global.security.token;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface UserTokenRepository {

    @Select("SELECT * FROM user_tokens WHERE user_id = #{userId}")
    Optional<UserToken> findByUserId(Long userId);

    @Update("UPDATE user_tokens SET refresh_token = #{refreshToken}, expiry_date = #{expiryDate}, created_at = CURRENT_TIMESTAMP() WHERE id = #{id}")
    void updateById(Long id, String refreshToken, String expiryDate);

    /// `UNIQUE (user_id)` + upsert. 호출자가 read-then-write 패턴이라 동시 로그인 시
    /// 두 스레드가 모두 "행 없음"을 보고 `save`에 진입한다. 단순 INSERT면 행이 2개가 되어
    /// `findByUserId`의 `Optional`이 `TooManyResultsException`으로 터지고
    /// **해당 유저의 로그인·재발급이 영구 500**이 된다.
    @Insert("INSERT INTO user_tokens (user_id, refresh_token, expiry_date) "
            + "VALUES (#{userId}, #{refreshToken}, #{expiryDate}) "
            + "ON DUPLICATE KEY UPDATE refresh_token = VALUES(refresh_token), "
            + "expiry_date = VALUES(expiry_date), created_at = CURRENT_TIMESTAMP()")
    void save(UserToken userToken);

    /// 해당 유저의 refresh token을 삭제하는 메서드
    ///
    /// 로그아웃과 탈퇴 신청 시 호출한다. 행을 지우면 이후 재발급 요청이 거부된다.
    ///
    /// @param userId 삭제할 유저 ID
    @Delete("DELETE FROM user_tokens WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}
