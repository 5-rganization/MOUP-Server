package com.moup.domain.alarm.application;

import com.moup.domain.alarm.domain.Notification;
import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.domain.NormalAlarm;
import com.moup.domain.alarm.exception.AlarmAlreadyReadException;
import com.moup.domain.alarm.exception.AlarmNotFoundException;
import com.moup.domain.alarm.mapper.AlarmRepository;
import com.moup.domain.user.domain.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moup.domain.user.mapper.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

  private static final int BATCH_SIZE = 1000;
  private final AlarmRepository alarmRepository;
  private final UserRepository userRepository;

  public List<Notification> findAllNotifications(Long userId) {
    List<NormalAlarm> normalAlarms = alarmRepository.findAllNormalAlarmsByUserId(userId);

    // 빈 알림함은 오류가 아니다. 404를 주면 클라이언트가 "서버 오류"와 "알림 없음"을
    // 구분할 수 없고, 신규 가입자는 알림함을 열자마자 에러를 본다.

    List<Notification> notifications = new ArrayList<>();
    for (NormalAlarm normalAlarm : normalAlarms) {
      notifications.add(Notification.builder()
          .id(normalAlarm.getId())
          .senderId(normalAlarm.getSenderId())
          .receiverId(normalAlarm.getReceiverId())
          .title(normalAlarm.getTitle())
          .content(normalAlarm.getContent())
          .sentAt(normalAlarm.getSentAt())
          .readAt(normalAlarm.getReadAt())
          .build());
    }

    return notifications;
  }

  /**
   * id 기준으로 normalAlarm 반환, receiverId가 userId에 해당해야 조회 가능.
   *
   * @param userId
   * @param notificationId
   * @return
   */
  public Notification findNotificationById(Long userId, Long notificationId) {
    NormalAlarm normalAlarm = alarmRepository.findNormalAlarmById(userId, notificationId)
        .orElseThrow(AlarmNotFoundException::new);

    return Notification.builder()
        .id(normalAlarm.getId())
        .senderId(normalAlarm.getSenderId())
        .receiverId(normalAlarm.getReceiverId())
        .title(normalAlarm.getTitle())
        .content(normalAlarm.getContent())
        .sentAt(normalAlarm.getSentAt())
        .readAt(normalAlarm.getReadAt())
        .build();
  }

  @Transactional
  public Notification readNotificationById(Long userId, Long notificationId) {
    // 읽음 여부 확인
    NormalAlarm normalAlarm = alarmRepository.findNormalAlarmById(userId, notificationId)
        .orElseThrow(AlarmNotFoundException::new);

    if (normalAlarm.getReadAt() != null) {
      throw new AlarmAlreadyReadException();
    }

    LocalDateTime readTime = LocalDateTime.now(SEOUL_ZONE_ID);

    alarmRepository.updateReadAtById(userId, notificationId, readTime);

    return Notification.builder()
        .id(normalAlarm.getId())
        .senderId(normalAlarm.getSenderId())
        .receiverId(normalAlarm.getReceiverId())
        .title(normalAlarm.getTitle())
        .content(normalAlarm.getContent())
        .sentAt(normalAlarm.getSentAt())
        .readAt(readTime)
        .build();
  }

  /**
   * 일반 알림을 삭제. receiver_id가 본인일 떄 삭제.
   *
   * @param userId
   * @param notificationId
   */
  public void deleteNotificationById(Long userId, Long notificationId) {

    alarmRepository.findNormalAlarmById(userId, notificationId)
        .orElseThrow(AlarmNotFoundException::new);

    alarmRepository.deleteNormalAlarmById(notificationId);
  }

  @Transactional
  public void readAllNotification(Long userId) {
    alarmRepository.updateAllReadAtByUserId(userId);
  }

  @Transactional
  public void deleteAllNotifications(Long userId) {
    alarmRepository.deleteAllNormalAlarmByUserId(userId);
  }

  /// 공지 대상 매핑 생성.
  ///
  /// **`@Async`를 떼고 호출자 트랜잭션에 참여시킨다.** 비동기로 돌면 아직 커밋되지 않은
  /// `admin_alarms` 행을 FK로 참조해 잠금 대기에 걸리고, 실패해도 `void`라 예외가 삼켜졌다.
  ///
  /// ponytail: 전 사용자를 한 트랜잭션에 담는다. 사용자가 수만 명을 넘어 요청이 길어지면
  /// 배치별 REQUIRES_NEW로 쪼개고 커밋 이후 비동기 실행으로 옮길 것.
  @Transactional
  public void createAnnouncementMappingForAllUsers(Long announcementId) {
    log.info("공지 대상 매핑 생성 시작 announcementId={}", announcementId);

    int page = 0;
    List<User> users;
    do {
      int offset = page * BATCH_SIZE;

      users = userRepository.findUsersWithPaging(offset, BATCH_SIZE);

      if (!users.isEmpty()) {
        alarmRepository.saveAnnouncementMappingForAllUsers(announcementId, users);
        page++;
      }
    } while (!users.isEmpty());

    log.info("공지 대상 매핑 생성 완료 announcementId={}", announcementId);
  }

  @Transactional
  public List<Announcement> findAllAnnouncements(Long userId) {
    List<AdminAlarm> adminAlarms = alarmRepository.findAllAdminAlarmsByUserId(userId);

    // 빈 공지함도 마찬가지다. 목록 조회는 빈 배열 + 200이어야 한다.

    List<Announcement> announcements = new ArrayList<>();
    for (AdminAlarm adminAlarm : adminAlarms) {
      announcements.add(
          Announcement.builder().id(adminAlarm.getId()).title(adminAlarm.getTitle()).content(
              adminAlarm.getContent()).sentAt(adminAlarm.getSentAt()).build());
    }

    return announcements;
  }

  public Announcement findAnnouncementById(Long userId, Long announcementId) {
    AdminAlarm adminAlarm = alarmRepository.findAdminAlarmById(userId, announcementId)
        .orElseThrow(AlarmNotFoundException::new);

    return Announcement.builder()
        .id(adminAlarm.getId())
        .title(adminAlarm.getTitle())
        .content(adminAlarm.getContent())
        .sentAt(adminAlarm.getSentAt())
        .build();
  }

  public void readAnnouncementById(Long userId, Long announcementId) {
    alarmRepository.updateAnnouncementReadAtById(userId, announcementId);
  }

  public void readAllAnnouncements(Long userId) {
    alarmRepository.updateAllAnnouncementReadAtByUserId(userId);
  }

  public void deleteAnnouncementById(Long userId, Long announcementId) {
    alarmRepository.updateAnnouncementDeletedAtById(userId, announcementId);
  }

  public void deleteAllAnnouncements(Long userId) {
    alarmRepository.updateAllAnnouncementDeletedAtByUserId(userId);
  }
}
