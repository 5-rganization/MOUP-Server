package com.moup.server.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moup.domain.user.dto.OwnerWorkplaceUpdateRequest;
import com.moup.domain.workplace.domain.WorkplaceCategory;
import com.moup.domain.workplace.dto.BaseWorkplaceUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/// 근무지 카테고리를 표시용 한글에서 영문 코드로 바꾼 것에 대한 회귀 테스트.
///
/// 예전에는 `categoryName`이 검증 없는 `String`이라 화면 표시용 한글("편의점")이
/// 그대로 저장됐다. 표시명이 DB에 들어가면 다국어 대응이 막히고, 클라이언트가 문구를
/// 한 글자만 바꿔도 기존 데이터와 매칭되지 않는다.
public class WorkplaceCategoryTest {

  private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

  private BaseWorkplaceUpdateRequest read(String categoryJson) throws Exception {
    return mapper.readValue(
        "{\"workplaceName\":\"세븐일레븐\"," + categoryJson + "\"ownerBasedLabelColor\":\"BLUE\"}",
        BaseWorkplaceUpdateRequest.class);
  }

  @Test
  @DisplayName("코드값을 받는다")
  void 코드값_수용() throws Exception {
    assertEquals(WorkplaceCategory.CVS, read("\"categoryName\":\"CVS\",").getCategoryName());
  }

  /// 이 테스트가 실패하면 표시명이 다시 DB로 새고 있다는 뜻이다.
  @Test
  @DisplayName("표시용 한글은 거부된다")
  void 한글_표시명_거부() {
    assertThrows(Exception.class, () -> read("\"categoryName\":\"편의점\","));
  }

  @Test
  @DisplayName("허용 목록 밖의 코드도 거부된다")
  void 알_수_없는_코드_거부() {
    assertThrows(Exception.class, () -> read("\"categoryName\":\"PHARMACY\","));
  }

  /// PATCH라 생략은 "변경 없음"이다. 열거형이라도 필수가 되면 안 된다.
  @Test
  @DisplayName("수정 요청에서 생략하면 null이다")
  void 생략은_변경없음() throws Exception {
    BaseWorkplaceUpdateRequest request = read("");
    assertInstanceOf(OwnerWorkplaceUpdateRequest.class, request);
    assertNull(request.getCategoryName(), "생략을 필수 오류로 만들면 라벨 색상만 바꾸는 요청이 막힌다");
  }

  /// **스키마 폭이 가장 긴 코드보다 짧으면 저장 시점에 잘리거나 실패한다.**
  /// 전환 전 `VARCHAR(10)`이었고 `MOVIE_THEATER`가 13자였다 — 코드로 바꾸는 순간
  /// 영화관만 저장되지 않는 상태가 됐을 것이다. 열거형이 늘어날 때도 여기서 걸린다.
  @Test
  @DisplayName("category_name 컬럼이 가장 긴 코드를 담을 수 있다")
  void 스키마_폭이_충분하다() throws Exception {
    String ddl = Files.readString(Path.of("../db/moup.sql"));
    Matcher m = Pattern.compile("`category_name`\\s+VARCHAR\\((\\d+)\\)").matcher(ddl);
    assertTrue(m.find(), "db/moup.sql에서 category_name 컬럼 정의를 찾지 못했다");

    int columnWidth = Integer.parseInt(m.group(1));
    int longest = 0;
    String longestName = "";
    for (WorkplaceCategory category : WorkplaceCategory.values()) {
      if (category.name().length() > longest) {
        longest = category.name().length();
        longestName = category.name();
      }
    }
    assertTrue(columnWidth >= longest,
        "VARCHAR(" + columnWidth + ")로는 " + longestName + "(" + longest + "자)를 담을 수 없다");
  }
}
