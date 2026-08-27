package com.moup.domain.user.application;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepository;
  private final UserDeletionService userDeletionService;
  private final FCMService fCMService;

  @Value("${user.delete.grace-period}")
  private int gracePeriod;

  @Value("${user.delete.revoke-give-up-period}")
  private int revokeGiveUpPeriod;

  /// 매일 새벽 4시, 유예기간이 지난 탈퇴 사용자를 영구 삭제한다.
  ///
  /// `/admin/users` 엔드포인트도 같은 작업을 그대로 노출한다 — 수동 실행 수단은
  /// 남겨 둔다. 다만 정기 실행이 외부 cron에 의존하지 않는다는 점이 핵심이다.
  /// [SchedulerConfig]에 그 이유를 적어 뒀다.
  ///
  /// 시작·종료를 로그로 남긴다. 이 배치는 **아무도 안 부르면 조용히 멈추는** 종류라,
  /// 돌았다는 증거가 없으면 안 돈 것과 구분할 수 없다. 실제로 그렇게 놓쳤다.
  @Scheduled(cron = "${user.delete.cron:0 0 4 * * *}", zone = "Asia/Seoul")
  public void hardDeleteOldUsers() {
    log.info("탈퇴 사용자 영구 삭제 배치 시작");
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
    log.info("탈퇴 사용자 영구 삭제 배치 종료");
  }

  public void hardDeleteUsersImmediately() {
    // 모든 하드 삭제 대상 유저 목록 조회
    List<User> hardDeleteUsers = userRepository.findAllHardDeleteUsers();

    for (User user : hardDeleteUsers) {
      userDeletionService.processUserDeletion(user);
    }
  }

  public void announce(AdminAnnouncementRequest adminAnnouncementRequest) {
    fCMService.sendToTopic(FCMTopic.ADMIN_ALARM, adminAnnouncementRequest.getTitle(),
        adminAnnouncementRequest.getContent());
  }

  @Transactional
  public void notify(Long adminId, AdminNotificationRequest adminNotificationRequest) {
    // 존재하지 않는 수신자에게 보내려 하면 404를 준다. 알림 내역의 FK 위반을 미리 막는다.
    userRepository.findById(adminNotificationRequest.getReceiverId())
        .orElseThrow(UserNotFoundException::new);

    fCMService.sendToSingleUser(adminId, adminNotificationRequest.getReceiverId(),
        adminNotificationRequest.getTitle(),
        adminNotificationRequest.getContent(), null);
  }
}
