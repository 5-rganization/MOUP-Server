package com.moup.domain.alarm.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import com.moup.domain.alarm.mapper.AdminAlarmRepository;
import com.moup.domain.alarm.mapper.AdminAlarmUserMappingRepository;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import com.moup.global.common.type.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({AlarmService.class, AnnouncementCreatedEventHandler.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AlarmServiceIntegrationTest {

  @Autowired
  private AlarmService alarmService;

  @Autowired
  private AdminAlarmRepository adminAlarmRepository;

  @Autowired
  private AdminAlarmUserMappingRepository mappingRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("단건 공지 읽음 처리는 서비스 트랜잭션 종료 후에도 반영된다")
  void readAnnouncementPersistsAfterServiceTransaction() {
    Fixture fixture = createFixture("read-user");

    alarmService.readAnnouncementById(fixture.userId(), fixture.announcementId());

    assertThat(mappingTimestamp(fixture.mappingId(), "read_at")).isNotNull();
  }

  @Test
  @DisplayName("단건 공지 삭제는 서비스 트랜잭션 종료 후에도 soft delete 상태를 유지한다")
  void deleteAnnouncementPersistsAfterServiceTransaction() {
    Fixture fixture = createFixture("delete-user");

    alarmService.deleteAnnouncementById(fixture.userId(), fixture.announcementId());

    assertThat(mappingTimestamp(fixture.mappingId(), "deleted_at")).isNotNull();
  }

  @Test
  @DisplayName("전체 공지 읽음 벌크 쿼리는 서비스 트랜잭션 안에서 실행된다")
  void readAllAnnouncementsRunsInTransaction() {
    Fixture first = createFixture("bulk-user");
    createAdditionalMapping(first.userId(), "두 번째 공지");

    alarmService.readAllAnnouncements(first.userId());

    Integer unreadCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM admin_alarm_user_mappings
            WHERE user_id = ? AND read_at IS NULL
            """,
        Integer.class,
        first.userId()
    );
    assertThat(unreadCount).isZero();
  }

  @Test
  @DisplayName("공지 생성 커밋 후 모든 사용자 매핑을 별도 트랜잭션에서 생성한다")
  void createAnnouncementMappingsAfterCommit() {
    transactionTemplate.executeWithoutResult(status -> {
      persistUser("event-user-1");
      persistUser("event-user-2");
      entityManager.flush();
    });
    Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

    Long announcementId = alarmService.createAnnouncement("전체 공지", "공지 내용");

    Integer mappingCount = awaitMappingCount(announcementId, userCount);
    assertThat(mappingCount).isEqualTo(userCount);
  }

  private int awaitMappingCount(Long announcementId, int expectedCount) {
    long deadline = System.currentTimeMillis() + 2_000;
    int count;
    do {
      count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM admin_alarm_user_mappings WHERE alarm_id = ?",
          Integer.class,
          announcementId
      );
      if (count == expectedCount) {
        return count;
      }
      try {
        Thread.sleep(25);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("공지 매핑 생성을 기다리는 중 인터럽트되었습니다.", exception);
      }
    } while (System.currentTimeMillis() < deadline);
    return count;
  }

  private Fixture createFixture(String username) {
    return transactionTemplate.execute(status -> {
      User user = persistUser(username);
      AdminAlarm announcement = adminAlarmRepository.save(
          AdminAlarm.builder().title("공지").content("공지 내용").build()
      );
      AdminAlarmUserMapping mapping = mappingRepository.save(
          AdminAlarmUserMapping.builder()
              .user(user)
              .adminAlarm(announcement)
              .build()
      );
      entityManager.flush();
      return new Fixture(user.getId(), announcement.getId(), mapping.getId());
    });
  }

  private void createAdditionalMapping(Long userId, String title) {
    transactionTemplate.executeWithoutResult(status -> {
      User user = entityManager.getReference(User.class, userId);
      AdminAlarm announcement = adminAlarmRepository.save(
          AdminAlarm.builder().title(title).content(title + " 내용").build()
      );
      mappingRepository.save(
          AdminAlarmUserMapping.builder()
              .user(user)
              .adminAlarm(announcement)
              .build()
      );
      entityManager.flush();
    });
  }

  private User persistUser(String username) {
    User user = User.builder()
        .provider(Login.LOGIN_APPLE)
        .providerId("provider_" + username)
        .username(username)
        .nickname(username)
        .role(Role.ROLE_WORKER)
        .build();
    entityManager.persist(user);
    return user;
  }

  private Object mappingTimestamp(Long mappingId, String column) {
    return jdbcTemplate.queryForObject(
        "SELECT " + column + " FROM admin_alarm_user_mappings WHERE id = ?",
        Object.class,
        mappingId
    );
  }

  private record Fixture(Long userId, Long announcementId, Long mappingId) {
  }
}
