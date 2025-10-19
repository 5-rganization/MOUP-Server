package com.moup.server.service;

import com.moup.server.exception.AlarmAlreadyReadException;
import com.moup.server.exception.AlarmNotFoundException;
import com.moup.server.model.entity.*;
import com.moup.server.repository.AlarmRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moup.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private static final int BATCH_SIZE = 1000;
    private final UserRepository userRepository;

    public List<Notification> findAllNotifications(Long userId) {
        List<NormalAlarm> normalAlarms = alarmRepository.findAllNormalAlarmsByUserId(userId);

        if (normalAlarms.isEmpty()) {
            throw new AlarmNotFoundException();
        }

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

        LocalDateTime readTime = LocalDateTime.now();

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

    @Async
    @Transactional
    public void createAnnouncementMappingForAllUsers(Long announcementId) {
        System.out.println(Thread.currentThread().getName() + ": Start creating announcement statuses for announcementId: " + announcementId);

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

        System.out.println(Thread.currentThread().getName() + ": Finished creating statuses.");
    }

    @Transactional
    public List<Announcement> findAllAnnouncements(Long userId) {
        List<AdminAlarm> adminAlarms = alarmRepository.findAllAdminAlarms();

        if (adminAlarms.isEmpty()) {
            throw new AlarmNotFoundException();
        }

        List<Announcement> announcements = new ArrayList<>();
        for (AdminAlarm adminAlarm : adminAlarms) {
            announcements.add(
                    Announcement.builder().id(adminAlarm.getId()).title(adminAlarm.getTitle()).content(
                            adminAlarm.getContent()).sentAt(adminAlarm.getSentAt()).build());
        }

        return announcements;
    }

//    public Announcement findAnnouncementById(Long userId, String announcementId) {
//
//    }
}
