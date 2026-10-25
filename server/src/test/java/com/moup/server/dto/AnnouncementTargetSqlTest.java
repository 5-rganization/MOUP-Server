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
}
