package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import com.moup.domain.alarm.domain.NormalAlarm;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import com.moup.global.common.type.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 설정만 로드하여 테스트 속도가 빠르고, 롤백이 기본적으로 적용됩니다.
class AlarmRepositoryTest {

  @Autowired
  private NormalAlarmRepository normalAlarmRepository;

  @Autowired
  private AdminAlarmRepository adminAlarmRepository;

  @Autowired
  private AdminAlarmUserMappingRepository mappingRepository;

  @Autowired
  private EntityManager em; // DB에 직접 쿼리를 날리거나 캐시를 관리하는 영속성 컨텍스트 매니저

  // 🔥 테스트용 유저를 DB에 저장하고 영속화된 객체를 반환하는 헬퍼 메서드
  private User createAndSaveUser(String username) {
    User user = User.builder()
        .provider(Login.LOGIN_APPLE)
        .providerId("provider_" + username)
        .username(username)
        .nickname(username)
        .role(Role.ROLE_WORKER)
        .build();
    em.persist(user); // JPA 방식으로 강제 INSERT
    return user;
  }

  // ==========================================
  // 1. NormalAlarm (일반 알림) 테스트
  // ==========================================

  @Test
  @DisplayName("일반 알림: 저장 및 단건/다건 조회 테스트")
  void saveAndFindNormalAlarm() {
    // given
    User sender = createAndSaveUser("sender");
    User receiver = createAndSaveUser("receiver");

    NormalAlarm alarm = NormalAlarm.builder()
        .sender(sender)
        .receiver(receiver)
        .title("근무 요청")
        .content("카페 대타 구합니다.")
        .build();

    // when
    NormalAlarm savedAlarm = normalAlarmRepository.save(alarm);
    List<NormalAlarm> alarms = normalAlarmRepository.findAllByReceiverId(receiver.getId());

    // then
    assertThat(savedAlarm.getId()).isNotNull();
    assertThat(alarms).hasSize(1);
    assertThat(alarms.get(0).getTitle()).isEqualTo("근무 요청");

    // 단건 조회 테스트
    Optional<NormalAlarm> foundAlarm = normalAlarmRepository.findByIdAndReceiverId(savedAlarm.getId(), receiver.getId());
    assertThat(foundAlarm).isPresent();
  }

  @Test
  @DisplayName("일반 알림: 벌크 연산으로 모두 읽음 처리 테스트 (더티 체킹 X, 쿼리 직접 실행)")
  void markAllNormalAlarmsAsRead() {
    // given
    User receiver = createAndSaveUser("receiver");
    normalAlarmRepository.save(NormalAlarm.builder().receiver(receiver).title("알림1").build());
    normalAlarmRepository.save(NormalAlarm.builder().receiver(receiver).title("알림2").build());

    // when
    normalAlarmRepository.markAllAsReadByUserId(receiver.getId());

    // 🔥 [핵심] 벌크 연산 직후에는 영속성 컨텍스트(캐시)를 비워줘야 DB에서 최신 상태를 제대로 불러옵니다.
    em.flush();
    em.clear();

    // then
    List<NormalAlarm> alarms = normalAlarmRepository.findAllByReceiverId(receiver.getId());
    assertThat(alarms).hasSize(2);
    assertThat(alarms).allMatch(a -> a.getReadAt() != null); // 모두 readAt이 채워졌는지 검증
  }

  // ==========================================
  // 2. AdminAlarm & Mapping (공지사항) 테스트
  // ==========================================

  @Test
  @DisplayName("공지사항: 대량 매핑 저장 및 JOIN FETCH 조회 테스트")
  void saveAllAndFetchJoinAdminAlarms() {
    // given: 공지사항 원본 저장
    AdminAlarm adminAlarm = AdminAlarm.builder()
        .title("전체 필독 공지")
        .content("JPA 마이그레이션 완료 안내")
        .build();
    adminAlarmRepository.save(adminAlarm);

    // given: 유저 3명 생성
    User user1 = createAndSaveUser("user1");
    User user2 = createAndSaveUser("user2");
    User user3 = createAndSaveUser("user3");

    // given: 매핑 객체 리스트 생성
    List<AdminAlarmUserMapping> mappings = List.of(
        AdminAlarmUserMapping.builder().user(user1).adminAlarm(adminAlarm).build(),
        AdminAlarmUserMapping.builder().user(user2).adminAlarm(adminAlarm).build(),
        AdminAlarmUserMapping.builder().user(user3).adminAlarm(adminAlarm).build()
    );

    // when: JPA의 saveAll()을 이용한 일괄 저장
    mappingRepository.saveAll(mappings);

    // 영속성 컨텍스트 초기화 (저장 후 조회 시 JOIN FETCH 쿼리가 제대로 나가는지 확인하기 위함)
    em.flush();
    em.clear();

    // then: User1의 시점에서 조회 (이때 N+1 문제 없이 AdminAlarm이 한방 쿼리로 조인되어야 함)
    List<AdminAlarmUserMapping> user1Alarms = mappingRepository.findAllActiveByUserId(user1.getId());

    assertThat(user1Alarms).hasSize(1);
    // 프록시 객체가 아닌 실제 AdminAlarm 데이터를 들고 있는지 검증
    assertThat(user1Alarms.get(0).getAdminAlarm().getTitle()).isEqualTo("전체 필독 공지");
  }

  @Test
  @DisplayName("공지사항: 벌크 연산 삭제(Soft Delete) 시 조회에서 제외되는지 필터링 검증")
  void softDeleteAndFilterAdminAlarms() {
    // given
    User user = createAndSaveUser("user999");
    AdminAlarm notice1 = adminAlarmRepository.save(AdminAlarm.builder().title("살아있는 공지").build());
    AdminAlarm notice2 = adminAlarmRepository.save(AdminAlarm.builder().title("삭제될 공지").build());

    mappingRepository.save(AdminAlarmUserMapping.builder().user(user).adminAlarm(notice1).build());
    AdminAlarmUserMapping mapToDelete = mappingRepository.save(AdminAlarmUserMapping.builder().user(user).adminAlarm(notice2).build());

    // when: notice2 매핑에 대해 Soft Delete 처리 (객체의 delete 메서드 호출 -> 더티 체킹 유도)
    mapToDelete.delete();
    em.flush();
    em.clear();

    // then: Active(삭제되지 않은) 공지사항만 가져오는 쿼리 실행
    List<AdminAlarmUserMapping> activeMappings = mappingRepository.findAllActiveByUserId(user.getId());

    // 살아있는 공지 1개만 조회되어야 함
    assertThat(activeMappings).hasSize(1);
    assertThat(activeMappings.get(0).getAdminAlarm().getTitle()).isEqualTo("살아있는 공지");
  }
}
