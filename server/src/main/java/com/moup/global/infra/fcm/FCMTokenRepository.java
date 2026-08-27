package com.moup.global.infra.fcm;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FCMTokenRepository {

    /// FCM 등록 토큰은 **앱 설치 단위**로 발급된다. 같은 기기에서 계정을 바꿔 로그인하면
    /// 동일한 토큰 문자열이 다른 유저에게 온다. `UNIQUE (token)` + upsert로
    /// 토큰의 소유자를 새 유저에게 넘겨, 이전 유저에게 가던 알림이 남의 폰에 뜨지 않게 한다.
    @Insert("INSERT INTO fcm_tokens (user_id, token) VALUES (#{userId}, #{token}) "
            + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), updated_at = CURRENT_TIMESTAMP()")
    void save(Long userId, String token);

    @Select("SELECT token FROM fcm_tokens WHERE user_id = #{userId}")
    List<String> findAllTokensByUserId(Long userId);

    /// 특정 기기 한 대만 로그아웃시킬 때, 그리고 FCM이 죽은 토큰이라고 알려줬을 때 쓴다.
    @Delete("DELETE FROM fcm_tokens WHERE token = #{token}")
    void deleteByToken(String token);

    /// 해당 유저의 모든 기기에서 푸시를 끊는다.
    @Delete("DELETE FROM fcm_tokens WHERE user_id = #{userId}")
    void deleteAllByUserId(Long userId);
}
