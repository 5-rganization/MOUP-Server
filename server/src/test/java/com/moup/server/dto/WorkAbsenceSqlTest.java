package com.moup.server.dto;

import com.moup.domain.work.mapper.WorkRepository;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 결근(`works.is_absent`)이 쿼리 수준에서 지켜지는지 **렌더링된 SQL로** 고정한다.
/// Mockito로는 매퍼를 흉내 낼 뿐이라 WHERE 절이 빠져도 통과한다.
class WorkAbsenceSqlTest {

    private static String render(String statementId) {
        Configuration configuration = new Configuration();
        configuration.addMapper(WorkRepository.class);
        return configuration.getMappedStatement(WorkRepository.class.getName() + "." + statementId)
                .getBoundSql(null).getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("출근 대상 조회는 결근한 근무를 제외한다")
    void 출근_대상에서_결근_제외() {
        String sql = render("findEligibleWorkForClockIn");

        assertTrue(sql.contains("is_absent = 0"), "결근한 근무에 출근이 찍힌다: " + sql);
    }

    /// 근무 수정은 결근을 건드리지 않는다. 여기에 `is_absent`가 들어가면 수정 요청에 없는 값(null)으로
    /// 덮어써 시간만 고쳤는데 결근이 풀린다.
    @Test
    @DisplayName("근무 수정 UPDATE는 is_absent를 덮어쓰지 않는다")
    void 근무_수정이_결근을_유지한다() {
        String sql = render("update");

        assertFalse(sql.contains("is_absent"), "근무 수정이 결근 표시를 덮어쓴다: " + sql);
    }
}
