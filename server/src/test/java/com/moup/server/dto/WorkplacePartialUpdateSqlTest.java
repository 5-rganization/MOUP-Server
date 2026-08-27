package com.moup.server.dto;

import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// `WorkplaceRepository.update`가 만들어 내는 SQL을 **문자열 수준에서** 고정한다.
///
/// Mockito로는 매퍼를 흉내 낼 뿐이라 동적 SQL이 깨져도 통과한다. DB 없이
/// MyBatis `Configuration`만 세워 실제 렌더링 결과를 본다.
public class WorkplacePartialUpdateSqlTest {

  private static String renderUpdateSql(Workplace workplace) {
    // DataSource 없이도 SQL 렌더링은 가능하다 — 실행이 아니라 문자열 생성만 확인한다.
    Configuration configuration = new Configuration();
    configuration.addMapper(WorkplaceRepository.class);
    MappedStatement statement = configuration.getMappedStatement(
        WorkplaceRepository.class.getName() + ".update");
    BoundSql boundSql = statement.getBoundSql(workplace);
    return boundSql.getSql().replaceAll("\\s+", " ").trim();
  }

  @Test
  @DisplayName("생략된 필드는 SET 절에 나타나지 않는다")
  void 생략된_필드는_갱신되지_않는다() {
    // 이름만 바꾸는 요청 — 주소·좌표는 보내지 않았다
    String sql = renderUpdateSql(Workplace.builder()
        .id(1L).ownerId(2L).workplaceName("새 이름").build());

    assertAll(
        () -> assertTrue(sql.contains("workplace_name ="), "보낸 필드는 갱신된다: " + sql),
        () -> assertFalse(sql.contains("address"), "생략한 주소가 SET에 들어갔다: " + sql),
        () -> assertFalse(sql.contains("latitude"), "생략한 위도가 SET에 들어갔다: " + sql),
        () -> assertFalse(sql.contains("longitude"), "생략한 경도가 SET에 들어갔다: " + sql),
        () -> assertFalse(sql.contains("category_name"), "생략한 카테고리가 SET에 들어갔다: " + sql)
    );
  }

  @Test
  @DisplayName("모든 필드를 보내면 전부 갱신된다")
  void 전체_필드_전송() {
    String sql = renderUpdateSql(Workplace.builder()
        .id(1L).ownerId(2L)
        .workplaceName("이름").categoryName("편의점")
        .address("경기 화성시").latitude(37.2).longitude(127.07).build());

    assertAll(
        () -> assertTrue(sql.contains("workplace_name ="), sql),
        () -> assertTrue(sql.contains("category_name ="), sql),
        () -> assertTrue(sql.contains("address ="), sql),
        () -> assertTrue(sql.contains("latitude ="), sql),
        () -> assertTrue(sql.contains("longitude ="), sql)
    );
  }

  /// `<set>`은 후행 쉼표를 지운다. 이게 깨지면 `SET a = ?, WHERE`가 되어 문법 오류가 난다.
  @Test
  @DisplayName("SET 절과 WHERE 절 사이에 쉼표가 남지 않는다")
  void 후행_쉼표_제거() {
    String sql = renderUpdateSql(Workplace.builder()
        .id(1L).ownerId(2L).address("주소만 변경").build());

    assertFalse(sql.matches(".*,\\s*WHERE.*"), "후행 쉼표가 남았다: " + sql);
    assertTrue(sql.contains("WHERE id = ? AND owner_id = ?"), sql);
  }

  /// 소유자 조건은 절대 사라지면 안 된다 — 남의 근무지를 고칠 수 있게 된다.
  @Test
  @DisplayName("owner_id 조건은 어떤 조합에서도 유지된다")
  void 소유자_조건_유지() {
    assertTrue(renderUpdateSql(Workplace.builder()
        .id(1L).ownerId(2L).workplaceName("이름").build())
        .contains("owner_id = ?"));
  }

  /// 다섯 필드를 전부 생략하면 `<set>`이 비어 **문법적으로 깨진 SQL**이 나온다.
  /// `WorkplaceService.updateWorkplaceFields`가 이 경우 update를 호출하지 않는 이유다.
  /// 그 가드가 사라지면 라벨 색상만 바꾸는 요청이 500으로 터진다.
  @Test
  @DisplayName("모든 필드를 생략하면 SET 절이 비어 깨진 SQL이 된다")
  void 전체_생략시_SQL이_깨진다() {
    String sql = renderUpdateSql(Workplace.builder().id(1L).ownerId(2L).build());
    assertTrue(sql.matches("UPDATE workplaces\\s+WHERE.*"),
        "가드가 필요 없어졌다면 이 테스트를 지워도 된다: " + sql);
  }
}
