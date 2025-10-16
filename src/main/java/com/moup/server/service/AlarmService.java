package com.moup.server.service;

import com.moup.server.exception.AlarmAlreadyReadException;
import com.moup.server.exception.AlarmNotFoundException;
import com.moup.server.model.entity.AdminAlarm;
import com.moup.server.model.entity.Announcement;
import com.moup.server.model.entity.NormalAlarm;
import com.moup.server.model.entity.Notification;
import com.moup.server.repository.AlarmRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmService {

  private final AlarmRepository alarmRepository;

  @Transactional
  public List<Announcement> findAllAnnouncements() {
    List<AdminAlarm> adminAlarms = alarmRepository.findAllAdminAlarms();

    if (adminAlarms.isEmpty()) {
      throw new AlarmNotFoundException();
    }

    List<Announcement> announcements = new ArrayList<>();
    for (AdminAlarm adminAlarm : adminAlarms) {
      announcements.add(Announcement.builder().title(adminAlarm.getTitle()).content(
          adminAlarm.getContent()).sentAt(adminAlarm.getSentAt()).build());
    }

    return announcements;
  }

  public List<Notification> findAllNotifications(Long userId) {
    List<NormalAlarm> normalAlarms = alarmRepository.findAllNormalAlarmsByUserId(userId);

    if (normalAlarms.isEmpty()) {
      throw new AlarmNotFoundException();
    }

    List<Notification> notifications = new ArrayList<>();
    for (NormalAlarm normalAlarm : normalAlarms) {
      notifications.add(Notification.builder()
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

    LocalDateTime readTime = LocalDateTime.now();

    alarmRepository.updateReadAtById(userId, notificationId, readTime);

    return Notification.builder()
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
}
