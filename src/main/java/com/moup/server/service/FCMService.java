package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.moup.server.common.FCMTopic;
import com.moup.server.model.dto.AdminAlarmRequest;
import com.moup.server.model.dto.NormalAlarmRequest;
import com.moup.server.model.entity.User;
import com.moup.server.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FCMService {

  private final UserService userService;
  private final AlarmRepository alarmRepository;

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
  public void sendToSingleUser(Long senderId, Long receiverId, String title, String body)
      throws FirebaseMessagingException {
    // TODO: 송신자 데이터가 필요할 경우 사용하기 (없으면 빼서 DB 호출 최소화)
    User sender = userService.findUserById(senderId);   // 송신자 유저
    User receiver = userService.findUserById(receiverId);   // 수신자 유저
    String fcmToken = receiver.getFcmToken();   // 수신자 FCM 토큰

    Notification notification = Notification.builder()
        .setTitle(title)
        .setBody(body)
        // .setImage("url-to-image") // 이미지 추가 가능
        .build();

    Message message = Message.builder()
        .setToken(fcmToken) // 특정 기기(클라이언트)의 토큰
        .setNotification(notification)
        // .putData("key", "value") // 데이터 페이로드 추가 가능
        .build();

    alarmRepository.saveNormalAlarm(NormalAlarmRequest.builder()
        .senderId(senderId)
        .receiverId(receiverId)
        .title(title)
        .content(body)
        .build());

    String response = FirebaseMessaging.getInstance().send(message);
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

    alarmRepository.saveAdminAlarm(AdminAlarmRequest.builder()
        .title(title)
        .content(body)
        .build());

    // [3] FCM 서버에 메시지 전송 요청
    String response = FirebaseMessaging.getInstance().send(message);
    System.out.println("Successfully sent topic message: " + response);
  }
}
