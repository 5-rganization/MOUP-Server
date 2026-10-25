package com.moup.server.dto;

import com.moup.domain.user.mapper.UserRepository;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// 공지 대상에서 탈퇴자를 빼는지 렌더링된 SQL로 고정한다 (스코프 6 I7).
class AnnouncementTargetSqlTest {

    @Test
    @DisplayName("공지 대상 조회는 탈퇴한 사용자를 제외한다")
    void 탈퇴자_제외() {
        Configuration configuration = new Configuration();
        configuration.addMapper(UserRepository.class);
        String sql = configuration.getMappedStatement(UserRepository.class.getName() + ".findUsersWithPaging")
                .getBoundSql(Map.of("offset", 0, "batchSize", 10)).getSql();

        assertTrue(sql.contains("is_deleted = 0"), sql);
    }

    /// 정렬도 상한도 없어 알림이 쌓일수록 응답이 무한정 커지고 순서도 정해지지 않았다(스코프 6 I5).
    @Test
    @DisplayName("알림함 조회는 최근 순이고 상한이 있다")
    void 알림_정렬과_상한() {
        Configuration configuration = new Configuration();
        configuration.addMapper(com.moup.domain.alarm.mapper.AlarmRepository.class);
        for (String id : new String[]{"findAllNormalAlarmsByUserId", "findAllAdminAlarmsByUserId"}) {
            String sql = configuration.getMappedStatement(com.moup.domain.alarm.mapper.AlarmRepository.class.getName() + "." + id)
                    .getBoundSql(Map.of("userId", 1L)).getSql();
            assertTrue(sql.contains("ORDER BY") && sql.contains("DESC") && sql.contains("LIMIT"), id + ": " + sql);
        }
    }
}
