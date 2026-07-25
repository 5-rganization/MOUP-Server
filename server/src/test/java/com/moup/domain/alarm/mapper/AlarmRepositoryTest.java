package com.moup.domain.alarm.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import com.moup.domain.alarm.domain.NormalAlarm;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import com.moup.global.common.type.Role;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AlarmRepositoryTest {

  @Autowired
  private NormalAlarmRepository normalAlarmRepository;

  @Autowired
  private AdminAlarmRepository adminAlarmRepository;

  @Autowired
  private AdminAlarmUserMappingRepository mappingRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("일반 알림을 실제 운영 스키마 형태에 저장하고 수신자 기준으로 조회한다")
  void saveAndFindNormalAlarm() {
    User sender = persistUser("sender");
    User receiver = persistUser("receiver");

    NormalAlarm saved = normalAlarmRepository.save(
        NormalAlarm.builder()
            .sender(sender)
            .receiver(receiver)
            .title("근무 요청")
            .content("새 근무 요청이 있습니다.")
            .build()
    );
    flushAndClear();

    List<NormalAlarm> alarms = normalAlarmRepository.findAllByReceiverId(receiver.getId());

    assertThat(alarms).hasSize(1);
    assertThat(alarms.get(0)).extracting(
        NormalAlarm::getId,
        NormalAlarm::getTitle,
        alarm -> alarm.getSender().getId(),
        alarm -> alarm.getReceiver().getId()
    ).containsExactly(saved.getId(), "근무 요청", sender.getId(), receiver.getId());
    assertThat(alarms.get(0).getSentAt()).isNotNull();
  }

  @Test
  @DisplayName("수신자의 읽지 않은 일반 알림을 벌크 쿼리로 모두 읽음 처리한다")
  void markAllNormalAlarmsAsRead() {
    User sender = persistUser("sender");
    User receiver = persistUser("receiver");
    saveNormalAlarm(sender, receiver, "알림 1");
    saveNormalAlarm(sender, receiver, "알림 2");
    flushAndClear();

    normalAlarmRepository.markAllAsReadByUserId(receiver.getId());
    flushAndClear();

    assertThat(normalAlarmRepository.findAllByReceiverId(receiver.getId()))
        .hasSize(2)
        .allMatch(alarm -> alarm.getReadAt() != null);
  }

  @Test
  @DisplayName("공지 매핑 조회는 공지 본문을 fetch join하고 삭제된 매핑을 제외한다")
  void findActiveAnnouncementMappings() {
    User user = persistUser("worker");
    AdminAlarm active = saveAdminAlarm("활성 공지");
    AdminAlarm deleted = saveAdminAlarm("삭제 공지");
    mappingRepository.save(
        AdminAlarmUserMapping.builder().user(user).adminAlarm(active).build()
    );
    AdminAlarmUserMapping deletedMapping = mappingRepository.save(
        AdminAlarmUserMapping.builder().user(user).adminAlarm(deleted).build()
    );
    deletedMapping.delete();
    flushAndClear();

    List<AdminAlarmUserMapping> mappings =
        mappingRepository.findAllActiveByUserId(user.getId());

    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getAdminAlarm().getTitle()).isEqualTo("활성 공지");
    assertThat(mappings.get(0).getAdminAlarm().getSentAt()).isNotNull();
  }

  @Test
  @DisplayName("공지 매핑은 사용자 수와 관계없이 한 번의 insert-select로 생성한다")
  void createMappingsForAllUsers() {
    persistUser("user1");
    persistUser("user2");
    persistUser("user3");
    AdminAlarm announcement = saveAdminAlarm("전체 공지");
    flushAndClear();

    int createdCount = mappingRepository.createMappingsForAllUsers(announcement.getId());
    flushAndClear();

    assertThat(createdCount).isEqualTo(3);
    assertThat(mappingRepository.count()).isEqualTo(3);
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

  private AdminAlarm saveAdminAlarm(String title) {
    return adminAlarmRepository.save(
        AdminAlarm.builder().title(title).content(title + " 내용").build()
    );
  }

  private void saveNormalAlarm(User sender, User receiver, String title) {
    normalAlarmRepository.save(
        NormalAlarm.builder()
            .sender(sender)
            .receiver(receiver)
            .title(title)
            .build()
    );
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
