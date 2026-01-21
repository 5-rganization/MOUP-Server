package com.moup.global.infra.fcm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.moup.domain.alarm.application.AlarmService;
import com.moup.domain.alarm.dto.NormalAlarmRequest;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.user.domain.User;
import com.moup.domain.alarm.mapper.AlarmRepository;
import com.moup.domain.user.application.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

  private final UserService userService;
  private final AlarmRepository alarmRepository;
  private final AlarmService alarmService;
  private final ObjectMapper objectMapper;

  /**
   * 특정 사용자 한 명에게 알림을 보냅니다. (1대1)
   *
   * @param senderId   송신자 ID
   * @param receiverId 수신자 ID
   * @param title      알림 제목
   * @param body       알림 내용
   * @throws FirebaseMessagingException FCM 전송 실패 시
   */
  @Transactional
  public void sendToSingleUser(Long senderId, Long receiverId, String title, String body,
                               Object dataPayload)
          throws FirebaseMessagingException {

      User sender = userService.findUserById(senderId);
      User receiver = userService.findUserById(receiverId);
      String fcmToken = receiver.getFcmToken();

      // 1. [DB 저장] 토큰 유무와 상관없이 알림 내역은 먼저 저장 (히스토리 보존)
      alarmRepository.saveNormalAlarm(NormalAlarmRequest.builder()
              .senderId(senderId)
              .receiverId(receiverId)
              .title(title)
              .content(body)
              .build());

      // 2. [토큰 검사] 토큰이 없으면 여기서 종료 (푸시는 안 보냄)
      if (fcmToken == null || fcmToken.isBlank()) {
          log.warn("FCM 전송 스킵: 수신자(ID: {})의 FCM 토큰이 없습니다. (DB 저장은 완료)", receiverId);
          return;
      }

      // 3. [FCM 전송] 토큰이 있을 때만 실행
      Notification notification = Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build();

      Message.Builder messageBuilder = Message.builder()
              .setToken(fcmToken)
              .setNotification(notification);

      if (dataPayload != null) {
          try {
              Map<String, String> dataMap = objectMapper.convertValue(dataPayload,
                      new TypeReference<Map<String, String>>() {});
              messageBuilder.putAllData(dataMap);
          } catch (IllegalArgumentException e) {
              log.error(e.getMessage());
          }
      }

      String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
      System.out.println("Successfully sent message: " + response);
  }

  /**
   * 특정 토픽을 구독한 모든 사용자에게 알림을 보냅니다. (공지)
   *
   * @param topic 구독할 토픽 이름 (예: "ADMIN_ALARM")
   * @param title 알림 제목
   * @param body  알림 내용
   * @throws FirebaseMessagingException FCM 전송 실패 시
   */
  @Transactional
  public void sendToTopic(FCMTopic topic, String title, String body)
      throws FirebaseMessagingException {
    // [1] 알림 메시지 본문 구성
    Notification notification = Notification.builder()
        .setTitle(title)
        .setBody(body)
        .build();

    // [2] 특정 토픽(topic)을 대상으로 메시지 생성
    Message message = Message.builder()
        .setTopic(topic.toString())
        .setNotification(notification)
        // .putData("key", "value") // 데이터 페이로드 추가 가능
        .build();

    Announcement announcement = Announcement.builder()
        .title(title)
        .content(body)
        .build();

    alarmRepository.saveAdminAlarm(announcement);
    Long announcementId = announcement.getId();

    // [3] FCM 서버에 메시지 전송 요청
    String response = FirebaseMessaging.getInstance().send(message);
    System.out.println("Successfully sent topic message: " + response);

    alarmService.createAnnouncementMappingForAllUsers(announcementId);
  }
}
