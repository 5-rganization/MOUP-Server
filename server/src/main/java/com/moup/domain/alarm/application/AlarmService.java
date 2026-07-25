package com.moup.domain.alarm.application;

import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.domain.AnnouncementCreatedEvent;
import com.moup.domain.alarm.domain.NormalAlarm;
import com.moup.domain.alarm.domain.Notification;
import com.moup.domain.alarm.exception.AlarmAlreadyReadException;
import com.moup.domain.alarm.exception.AlarmNotFoundException;
import com.moup.domain.alarm.mapper.AdminAlarmRepository;
import com.moup.domain.alarm.mapper.AdminAlarmUserMappingRepository;
import com.moup.domain.alarm.mapper.NormalAlarmRepository;
import com.moup.domain.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

  private final NormalAlarmRepository normalAlarmRepository;
  private final AdminAlarmRepository adminAlarmRepository;
  private final AdminAlarmUserMappingRepository adminAlarmUserMappingRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void createNotification(User sender, User receiver, String title, String content) {
    normalAlarmRepository.save(
        NormalAlarm.builder()
            .sender(sender)
            .receiver(receiver)
            .title(title)
            .content(content)
            .build()
    );
  }

  @Transactional
  public Long createAnnouncement(String title, String content) {
    AdminAlarm savedAlarm = adminAlarmRepository.save(
        AdminAlarm.builder()
            .title(title)
            .content(content)
            .build()
    );
    eventPublisher.publishEvent(new AnnouncementCreatedEvent(savedAlarm.getId()));
    return savedAlarm.getId();
  }

  public List<Notification> findAllNotifications(Long userId) {
    List<NormalAlarm> normalAlarms = normalAlarmRepository.findAllByReceiverId(userId);
    if (normalAlarms.isEmpty()) {
      throw new AlarmNotFoundException();
    }
    return normalAlarms.stream().map(this::toNotification).toList();
  }

  public Notification findNotificationById(Long userId, Long notificationId) {
    NormalAlarm normalAlarm = normalAlarmRepository.findByIdAndReceiverId(notificationId, userId)
        .orElseThrow(AlarmNotFoundException::new);
    return toNotification(normalAlarm);
  }

  @Transactional
  public Notification readNotificationById(Long userId, Long notificationId) {
    NormalAlarm normalAlarm = normalAlarmRepository.findByIdAndReceiverId(notificationId, userId)
        .orElseThrow(AlarmNotFoundException::new);
    if (normalAlarm.getReadAt() != null) {
      throw new AlarmAlreadyReadException();
    }
    normalAlarm.read();
    return toNotification(normalAlarm);
  }

  @Transactional
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

  public List<Announcement> findAllAnnouncements(Long userId) {
    List<AdminAlarmUserMapping> mappings =
        adminAlarmUserMappingRepository.findAllActiveByUserId(userId);
    if (mappings.isEmpty()) {
      throw new AlarmNotFoundException();
    }
    return mappings.stream().map(this::toAnnouncement).toList();
  }

  public Announcement findAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping =
        adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
            .orElseThrow(AlarmNotFoundException::new);
    return toAnnouncement(mapping);
  }

  @Transactional
  public void readAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping =
        adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
            .orElseThrow(AlarmNotFoundException::new);
    mapping.read();
  }

  @Transactional
  public void readAllAnnouncements(Long userId) {
    adminAlarmUserMappingRepository.markAllAnnouncementsAsReadByUserId(userId);
  }

  @Transactional
  public void deleteAnnouncementById(Long userId, Long announcementId) {
    AdminAlarmUserMapping mapping =
        adminAlarmUserMappingRepository.findActiveByUserIdAndAlarmId(userId, announcementId)
            .orElseThrow(AlarmNotFoundException::new);
    mapping.delete();
  }

  @Transactional
  public void deleteAllAnnouncements(Long userId) {
    adminAlarmUserMappingRepository.softDeleteAllByUserId(userId);
  }

  private Notification toNotification(NormalAlarm alarm) {
    return Notification.builder()
        .id(alarm.getId())
        .senderId(alarm.getSender().getId())
        .receiverId(alarm.getReceiver().getId())
        .title(alarm.getTitle())
        .content(alarm.getContent())
        .sentAt(alarm.getSentAt())
        .readAt(alarm.getReadAt())
        .build();
  }

  private Announcement toAnnouncement(AdminAlarmUserMapping mapping) {
    AdminAlarm alarm = mapping.getAdminAlarm();
    return Announcement.builder()
        .id(alarm.getId())
        .title(alarm.getTitle())
        .content(alarm.getContent())
        .sentAt(alarm.getSentAt())
        .build();
  }
}
