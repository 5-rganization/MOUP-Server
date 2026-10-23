package com.moup.server.config;

import com.moup.domain.user.application.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/// 탈퇴 사용자 영구 삭제 배치가 **실제로 예약돼 있는지** 확인한다.
///
/// 이 배치는 예전에 라즈베리파이 cron이 돌렸는데, 스크립트가 경로에 없어
/// 한 번도 실행되지 않았고 아무도 몰랐다. 조용히 멈추는 종류의 작업이라
/// 스케줄이 사라져도 증상이 "탈퇴자 데이터가 안 지워진다" 하나뿐이고,
/// 그건 들여다보기 전에는 보이지 않는다. 그래서 여기서 못 박는다.
public class UserDeletionScheduleTest {

  private static Method hardDeleteOldUsers() throws Exception {
    return AdminService.class.getMethod("hardDeleteOldUsers");
  }

  @Test
  @DisplayName("hardDeleteOldUsers에 @Scheduled가 붙어 있다")
  void 예약되어_있다() throws Exception {
    Scheduled scheduled = hardDeleteOldUsers().getAnnotation(Scheduled.class);
    assertNotNull(scheduled,
        "@Scheduled가 사라지면 탈퇴자 데이터가 영원히 남는다 — 증상이 조용하다");
    assertEquals("Asia/Seoul", scheduled.zone(),
        "서버 타임존에 기대면 컨테이너 설정 변경에 배치 시각이 끌려간다");
  }

  @Test
  @DisplayName("cron 식이 유효하고 하루에 한 번 실행된다")
  void 하루에_한번_실행된다() throws Exception {
    // 기본값을 쓴다. 프로퍼티로 덮을 수 있으므로 플레이스홀더를 벗겨 낸다.
    String cron = hardDeleteOldUsers().getAnnotation(Scheduled.class).cron();
    String defaultCron = cron.replaceFirst("^\\$\\{[^:]+:", "").replaceFirst("}$", "");

    assertTrue(CronExpression.isValidExpression(defaultCron), "잘못된 cron 식: " + defaultCron);

    CronExpression expression = CronExpression.parse(defaultCron);
    LocalDateTime first = expression.next(LocalDateTime.of(2026, 1, 1, 0, 0));
    LocalDateTime second = expression.next(first);

    assertEquals(Duration.ofDays(1), Duration.between(first, second),
        "하루 간격이 아니다: " + first + " → " + second);
  }
}
