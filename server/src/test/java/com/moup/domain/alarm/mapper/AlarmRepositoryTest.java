package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AlarmRepositoryTest {

  @Autowired
  private AlarmRepository alarmRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  // DB에 가짜 유저를 꽂아넣는 헬퍼 메소드 (FK 제약조건 회피용)
  private void saveFakeUser(String providerId, Long userId) {
    jdbcTemplate.update(
        "INSERT INTO users (id, provider, provider_id, username) VALUES (?, ?, ?, ?)",
        userId, Login.LOGIN_APPLE.name(), providerId, "Tester" + userId);
  }

  @Test
  @DisplayName("🔥 복잡한 쿼리 1: 공지사항 대량 유저 매핑 (Batch Insert 검증)")
  void saveAnnouncementMappingForAllUsersTest() {
    // given (공지사항 생성 - Builder 사용)
    Announcement announcement = Announcement.builder()
        .title("전체 필독 공지")
        .content("서버 점검 안내")
        .build();

    alarmRepository.saveAnnouncement(announcement); // ID 생성됨

    // given (유저 DB Insert 및 객체 생성)
    saveFakeUser("1", 100L);
    saveFakeUser("2", 200L);
    saveFakeUser("3", 300L);

    List<User> targetUsers = List.of(
        User.builder().id(100L).build(),
        User.builder().id(200L).build(),
        User.builder().id(300L).build()
    );

    // when
    alarmRepository.saveAnnouncementMappingForAllUsers(announcement.getId(), targetUsers);

    // then
    List<AdminAlarm> resultA = alarmRepository.findAllAdminAlarmsByUserId(100L);
    assertThat(resultA).isNotEmpty();
    assertThat(resultA.get(0).getTitle()).isEqualTo("전체 필독 공지");
  }

  @Test
  @DisplayName("🔥 복잡한 쿼리 2: 공지사항 조회 (JOIN + Soft Delete 필터링 확인)")
  void findAllAdminAlarmsWithJoinAndFilterTest() {
    // given
    Long userId = 999L;
    saveFakeUser("4", userId); // DB에 유저 저장

    // 매핑에 사용할 유저 리스트 (Builder 사용)
    List<User> userList = List.of(
        User.builder().id(userId).build()
    );

    // 1. 활성 공지 생성 및 매핑 (Builder 사용)
    Announcement activeNotice = Announcement.builder()
        .title("살아있는 공지")
        .build();
    alarmRepository.saveAnnouncement(activeNotice);
    alarmRepository.saveAnnouncementMappingForAllUsers(activeNotice.getId(), userList);

    // 2. 삭제될 공지 생성 및 매핑 (Builder 사용)
    Announcement deletedNotice = Announcement.builder()
        .title("삭제된 공지")
        .build();
    alarmRepository.saveAnnouncement(deletedNotice);
    alarmRepository.saveAnnouncementMappingForAllUsers(deletedNotice.getId(), userList);

    // 3. 삭제 처리
    alarmRepository.updateAnnouncementDeletedAtById(userId, deletedNotice.getId());

    // when
    List<AdminAlarm> results = alarmRepository.findAllAdminAlarmsByUserId(userId);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getTitle()).isEqualTo("살아있는 공지");
    assertThat(results)
        .extracting(AdminAlarm::getTitle)
        .doesNotContain("삭제된 공지");
  }
}
