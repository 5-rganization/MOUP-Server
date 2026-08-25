package com.moup.domain.user.application;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.global.infra.fcm.FCMTopic;
import com.moup.domain.user.exception.UserNotFoundException;
import com.moup.domain.alarm.dto.AdminAnnouncementRequest;
import com.moup.domain.alarm.dto.AdminNotificationRequest;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.infra.fcm.FCMService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepository;
  private final UserDeletionService userDeletionService;
  private final FCMService fCMService;

  @Value("${user.delete.grace-period}")
  private int gracePeriod;

  @Value("${user.delete.revoke-give-up-period}")
  private int revokeGiveUpPeriod;

  public void hardDeleteOldUsers() {
    LocalDateTime now = LocalDateTime.now(SEOUL_ZONE_ID);
    LocalDateTime graceDeadline = now.minusDays(gracePeriod);
    LocalDateTime giveUpDeadline = now.minusDays(revokeGiveUpPeriod);

    // 1. 포기 기준을 넘긴 유저: 소셜 연동 해제를 더 시도하지 않고 기록만 남긴 뒤 삭제한다.
    //    이 상한이 없으면 revoke가 계속 실패하는 계정의 데이터가 영원히 남는다.
    for (User user : userRepository.findAllRevokeGiveUpUsers(giveUpDeadline)) {
      userDeletionService.forceDeleteAfterRevokeGiveUp(user, revokeGiveUpPeriod);
    }

    // 2. 유예기간이 지났고 아직 포기 기준에는 못 미친 유저: 정상 시도
    for (User user : userRepository.findAllOldHardDeleteUsers(graceDeadline, giveUpDeadline)) {
      userDeletionService.processUserDeletion(user);
    }
  }

  public void hardDeleteUsersImmediately() {
    // 모든 하드 삭제 대상 유저 목록 조회
    List<User> hardDeleteUsers = userRepository.findAllHardDeleteUsers();

    for (User user : hardDeleteUsers) {
      userDeletionService.processUserDeletion(user);
    }
  }

  public void announce(AdminAnnouncementRequest adminAnnouncementRequest)
      throws FirebaseMessagingException {
    fCMService.sendToTopic(FCMTopic.ADMIN_ALARM, adminAnnouncementRequest.getTitle(),
        adminAnnouncementRequest.getContent());
  }

  @Transactional
  public void notify(Long adminId, AdminNotificationRequest adminNotificationRequest)
      throws FirebaseMessagingException {
    User receiver = userRepository.findById(adminNotificationRequest.getReceiverId()).orElseThrow(
        UserNotFoundException::new);

    fCMService.sendToSingleUser(adminId, adminNotificationRequest.getReceiverId(),
        adminNotificationRequest.getTitle(),
        adminNotificationRequest.getContent(), null);
  }
}
