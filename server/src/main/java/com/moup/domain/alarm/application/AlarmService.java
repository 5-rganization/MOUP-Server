package com.moup.domain.alarm.application;

import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import com.moup.domain.alarm.domain.Notification;
import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.domain.NormalAlarm;
import com.moup.domain.alarm.exception.AlarmAlreadyReadException;
import com.moup.domain.alarm.exception.AlarmNotFoundException;
import com.moup.domain.alarm.mapper.AdminAlarmRepository;
import com.moup.domain.alarm.mapper.AdminAlarmUserMappingRepository;
import com.moup.domain.alarm.mapper.NormalAlarmRepository;
import com.moup.domain.user.domain.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moup.domain.user.mapper.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.global.common.domain.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class AlarmService {

  private static final int BATCH_SIZE = 1000;
  private final UserRepository userRepository;
  private final NormalAlarmRepository normalAlarmRepository;
  private final AdminAlarmRepository adminAlarmRepository;
  private final AdminAlarmUserMappingRepository adminAlarmUserMappingRepository;

  public List<Notification> findAllNotifications(Long userId) {
      List<NormalAlarm> normalAlarms = normalAlarmRepository.findAllByReceiverId(userId);

    if (normalAlarms.isEmpty()) {
      throw new AlarmNotFoundException();
    }

    List<Notification> notifications = new ArrayList<>();
    for (NormalAlarm normalAlarm : normalAlarms) {
      notifications.add(Notification.builder()
          .id(normalAlarm.getId())
          .senderId(normalAlarm.getSender().getId())
          .receiverId(normalAlarm.getReceiver().getId())
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
    NormalAlarm normalAlarm = normalAlarmRepository.findByIdAndReceiverId(notificationId, userId)
        .orElseThrow(AlarmNotFoundException::new);

    return Notification.builder()
        .id(normalAlarm.getId())
        .senderId(normalAlarm.getSender().getId())
        .receiverId(normalAlarm.getReceiver().getId())
        .title(normalAlarm.getTitle())
        .content(normalAlarm.getContent())
        .sentAt(normalAlarm.getSentAt())
        .readAt(normalAlarm.getReadAt())
        .build();
  }

  @Transactional
  public Notification readNotificationById(Long userId, Long notificationId) {
    // 읽음 여부 확인
    NormalAlarm normalAlarm = normalAlarmRepository.findByIdAndReceiverId(notificationId, userId)
        .orElseThrow(AlarmNotFoundException::new);

    if (normalAlarm.getReadAt() != null) {
      throw new AlarmAlreadyReadException();
    }

    LocalDateTime readTime = LocalDateTime.now(SEOUL_ZONE_ID);
    normalAlarm.read();

    return Notification.builder()
        .id(normalAlarm.getId())
        .senderId(normalAlarm.getSender().getId())
        .receiverId(normalAlarm.getReceiver().getId())
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

    normalAlarmRepository.findByIdAndReceiverId(notificationId, userId)
        .orElseThrow(AlarmNotFoundException::new);

    normalAlarmRepository.deleteById(notificationId);
  }

  @Transactional
  public void readAllNotification(Long userId) {
    normalAlarmRepository.markAllAsReadByUserId(userId);
  }

  @Transactional
  public void deleteAllNotifications(Long userId) {
    normalAlarmRepository.deleteAllByReceiverId(userId);
  }

  @Async
  @Transactional
  public void createAnnouncementMappingForAllUsers(Long announcementId) {
    System.out.println(Thread.currentThread().getName()
        + ": Start creating announcement statuses for announcementId: " + announcementId);

    AdminAlarm adminAlarm = adminAlarmRepository.findById(announcementId)
        .orElseThrow(AlarmNotFoundException::new);

    int page = 0;
    List<User> users;
    
    // 1000명씩 배치 적용
    do {
      int offset = page * BATCH_SIZE;
      users = userRepository.findUsersWithPaging(offset, BATCH_SIZE);

      if (!users.isEmpty()) {
        List<AdminAlarmUserMapping> mappings = new ArrayList<>();

        for (User user : users) {
          mappings.add(
              AdminAlarmUserMapping.builder()
                  .user(user)
                  .adminAlarm(adminAlarm)
                  .build()
          );
        }

        adminAlarmUserMappingRepository.saveAll(mappings);
        page++;
      }
    } while (!users.isEmpty());

    System.out.println(Thread.currentThread().getName() + ": Finished creating statuses.");
  }

  @Transactional
  public List<Announcement> findAllAnnouncements(Long userId) {
    List<AdminAlarmUserMapping> mappings = adminAlarmUserMappingRepository.findAllActiveByUserId(userId);

    if (mappings.isEmpty()) {
      throw new AlarmNotFoundException();
    }

    List<Announcement> announcements = new ArrayList<>();

    for (AdminAlarmUserMapping mapping : mappings) {
      AdminAlarm adminAlarm = mapping.getAdminAlarm();

      announcements.add(
          Announcement.builder()
              .id(adminAlarm.getId())
              .title(adminAlarm.getTitle())
              .content(adminAlarm.getContent())
              .sentAt(adminAlarm.getSentAt())
              .build());
    }

    return announcements;
  }

  public Announcement findAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping = adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
        .orElseThrow(AlarmNotFoundException::new);

    AdminAlarm adminAlarm = mapping.getAdminAlarm();

    return Announcement.builder()
        .id(adminAlarm.getId())
        .title(adminAlarm.getTitle())
        .content(adminAlarm.getContent())
        .sentAt(adminAlarm.getSentAt())
        .build();
  }

  public void readAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping = adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
        .orElseThrow(AlarmNotFoundException::new);

    mapping.read();

  }

  public void readAllAnnouncements(Long userId) {
    adminAlarmUserMappingRepository.markAllAnnouncementsAsReadByUserId(userId);
  }

  public void deleteAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping = adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
        .orElseThrow(AlarmNotFoundException::new);

    mapping.delete();
  }

  public void deleteAllAnnouncements(Long userId) {
    adminAlarmUserMappingRepository.softDeleteAllByUserId(userId);
  }
}
