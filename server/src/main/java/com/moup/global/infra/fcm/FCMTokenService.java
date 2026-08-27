package com.moup.global.infra.fcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMTokenService {

    private final FCMTokenRepository fcmTokenRepository;
    private final FCMService fcmService;

    /// 토큰 등록·갱신.
    ///
    /// **빈 값은 저장하지 않는다.** 로그인 요청의 `fcmToken`은 선택 항목이라
    /// 클라이언트가 생략하거나 SDK 초기화가 늦어 null로 올 수 있는데, 그대로 저장하면
    /// 기존 토큰이 지워져 **푸시가 조용히 끊긴다.** 서버는 200을 반환하고 에러도 없어
    /// 아무도 눈치채지 못한다. 토큰 삭제는 로그아웃/탈퇴 경로로만 한다.
    @Transactional
    public void updateUserFCMToken(Long userId, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM 토큰이 비어 있어 저장하지 않습니다. userId={}", userId);
            return;
        }
        fcmTokenRepository.save(userId, fcmToken);
        // 관리자 공지는 토픽 발송이라 구독하지 않으면 아무에게도 도달하지 않는다.
        // 클라이언트가 subscribeToTopic을 호출하지 않으므로 서버가 대신 구독시킨다.
        fcmService.subscribeToAdminTopic(fcmToken);
    }

    /// 기기 한 대만 로그아웃. 토큰을 모르면 전 기기를 끊는다.
    @Transactional
    public void deleteUserFCMToken(Long userId, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            fcmTokenRepository.deleteAllByUserId(userId);
            return;
        }
        fcmTokenRepository.deleteByToken(fcmToken);
    }

    @Transactional
    public void deleteAllUserFCMTokens(Long userId) {
        fcmTokenRepository.deleteAllByUserId(userId);
    }
}
