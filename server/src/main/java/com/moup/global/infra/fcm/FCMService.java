package com.moup.global.infra.fcm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.moup.domain.alarm.application.AlarmService;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.dto.NormalAlarmRequest;
import com.moup.domain.alarm.mapper.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// 푸시 알림 발송.
///
/// **푸시는 best-effort다.** 전송 실패가 비즈니스 작업을 되돌리지 않는다.
/// 이전 구현은 `@Transactional` 안에서 네트워크 호출을 하고 호출부가 예외를 다시 던져,
/// 죽은 토큰 하나 때문에 **근무자 승인·거부·근무지 참가가 영구히 불가능**해졌다.
/// 알림 내역은 `normal_alarms`에 남으므로 사용자는 앱 내 알림함에서 확인할 수 있다.
@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final AlarmRepository alarmRepository;
    private final AlarmService alarmService;
    private final FCMTokenRepository fcmTokenRepository;
    private final ObjectMapper objectMapper;

    /// 이 코드들이 오면 토큰이 되살아나지 않는다. 지우지 않으면 그 사용자 앞으로 가는
    /// 모든 발송이 영구히 실패한다.
    private static final Set<MessagingErrorCode> DEAD_TOKEN_CODES = EnumSet.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH);

    /// 특정 사용자의 **모든 기기**에 알림을 보낸다.
    ///
    /// 발신자·수신자를 `findUserById`로 조회하지 않는다. 그 메서드는 탈퇴 신청 유저에
    /// 예외를 던지므로, 조회하면 **사장님이 탈퇴 신청한 알바생을 근무지에서 뺄 수 없게 된다.**
    /// 유예기간 전면 차단은 본인의 기능을 막는 정책이지 제3자의 기능까지 막는 것이 아니다.
    public void sendToSingleUser(Long senderId, Long receiverId, String title, String body,
                                 Object dataPayload) {
        // 1. 알림 내역은 푸시 성공 여부와 무관하게 저장한다 (호출자 트랜잭션에 참여).
        alarmRepository.saveNormalAlarm(NormalAlarmRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .title(title)
                .content(body)
                .build());

        List<String> tokens = fcmTokenRepository.findAllTokensByUserId(receiverId);
        if (tokens.isEmpty()) {
            log.info("FCM 전송 스킵 - 등록된 기기가 없습니다. receiverId={} (DB 저장은 완료)", receiverId);
            return;
        }

        Map<String, String> data = toDataMap(dataPayload);
        // 2. 커밋 이후에 보낸다. 롤백된 작업의 알림이 나가지 않고,
        //    네트워크 대기가 DB 커넥션을 붙잡지 않는다.
        afterCommit(() -> push(tokens, title, body, data));
    }

    /// 전체 공지. 토픽 발송이므로 구독자에게만 도달한다
    /// (구독은 `FCMTokenService`가 토큰 등록 시 서버에서 처리한다).
    @Transactional
    public void sendToTopic(FCMTopic topic, String title, String body) {
        Announcement announcement = Announcement.builder()
                .title(title)
                .content(body)
                .build();
        alarmRepository.saveAdminAlarm(announcement);

        // 매핑을 **푸시보다 먼저, 같은 트랜잭션에서** 만든다.
        // 이전 구현은 푸시를 먼저 보내고 매핑 생성은 @Async로 넘겼다. 그래서
        // (a) 롤백되면 푸시는 떴는데 앱을 열면 404였고,
        // (b) 비동기 스레드가 아직 커밋되지 않은 부모 행을 FK로 참조해 잠금 대기에 걸렸다.
        alarmService.createAnnouncementMappingForAllUsers(announcement.getId());

        afterCommit(() -> pushTopic(topic, title, body));
    }

    /// 토큰 하나를 관리자 공지 토픽에 구독시킨다. 실패해도 로그인·토큰 갱신을 막지 않는다.
    public void subscribeToAdminTopic(String token) {
        afterCommit(() -> {
            try {
                FirebaseMessaging.getInstance()
                        .subscribeToTopic(List.of(token), FCMTopic.ADMIN_ALARM.toString());
            } catch (FirebaseMessagingException e) {
                log.warn("ADMIN_ALARM 토픽 구독 실패 - 이 기기는 전체 공지를 받지 못합니다. error={}",
                        e.getMessage());
            }
        });
    }

    // ========== 내부 ==========

    /// 기기별로 하나씩 보낸다.
    ///
    /// ponytail: 토큰 수만큼 HTTP 호출이 나간다. 사용자당 기기는 보통 1~3대라 문제가 없다.
    /// firebase-admin을 9.9.0으로 올렸으므로 `sendEachForMulticast`(호출 1회당 최대
    /// 500토큰, 토큰별 결과를 그대로 돌려주어 죽은 토큰 정리도 유지된다)를 쓸 수 있다.
    /// 지금은 N이 작아 이득이 없어 그대로 둔다. 한 번에 수십 대 이상으로 나가는
    /// 경로가 생기면 그때 바꿀 것.
    private void push(List<String> tokens, String title, String body, Map<String, String> data) {
        Notification notification = Notification.builder().setTitle(title).setBody(body).build();
        for (String token : tokens) {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);
            if (!data.isEmpty()) {
                builder.putAllData(data);
            }
            try {
                FirebaseMessaging.getInstance().send(builder.build());
            } catch (FirebaseMessagingException e) {
                // 푸시는 best-effort. 여기서 던지면 호출자의 비즈니스 작업이 되돌아간다.
                handleSendFailure(token, e);
            }
        }
    }

    private void pushTopic(FCMTopic topic, String title, String body) {
        Message message = Message.builder()
                .setTopic(topic.toString())
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("공지 토픽 전송 실패 topic={} error={}", topic, e.getMessage());
        }
    }

    /// FCM이 "이 토큰은 죽었다"고 알려주면 지운다. 이 정리가 없으면 앱을 지운 사용자의
    /// 죽은 토큰이 영구히 남아 그 사용자 앞으로 가는 모든 발송이 계속 실패한다.
    private void handleSendFailure(String token, FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code != null && DEAD_TOKEN_CODES.contains(code)) {
            fcmTokenRepository.deleteByToken(token);
            log.info("죽은 FCM 토큰을 정리했습니다. code={}", code);
            return;
        }
        log.error("FCM 전송 실패 - 알림 내역은 저장되어 앱에서 확인 가능합니다. code={} error={}",
                code, e.getMessage());
    }

    private Map<String, String> toDataMap(Object dataPayload) {
        if (dataPayload == null) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(dataPayload, new TypeReference<Map<String, String>>() {});
        } catch (IllegalArgumentException e) {
            log.error("FCM data payload 변환 실패 - 알림은 payload 없이 발송합니다. error={}", e.getMessage());
            return Map.of();
        }
    }

    /// 트랜잭션이 있으면 커밋 이후에, 없으면 즉시 실행한다.
    private void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
