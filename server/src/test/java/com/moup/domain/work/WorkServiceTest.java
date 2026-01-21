package com.moup.domain.work;

import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.dto.WorkDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 예시: WorkServiceTest.java
@SpringBootTest // 통합 테스트 (DB까지 연결)
class WorkServiceTest {

  @Autowired WorkService workService;

  @Test
  @DisplayName("근무 상세 조회 테스트")
  void getWorkDetailTest() {
    // given (데이터 준비)
    Long userId = 3L;
    Long workId = 195L;

    // when (실행)
    WorkDetailResponse result = workService.getWorkDetail(userId, workId);

    // then (검증)

  }
}
