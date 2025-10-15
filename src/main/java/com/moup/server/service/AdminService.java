package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.server.common.FCMTopic;
import com.moup.server.model.dto.AnnouncementRequest;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepository;
  private final UserDeletionService userDeletionService;
  private final FCMService fCMService;

  @Value("${user.delete.grace-period}")
  private int gracePeriod;

  public void hardDeleteOldUsers() {
    // 유예기간이 지난 하드 삭제 대상 유저 목록 조회
    LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(gracePeriod);
    List<User> hardDeleteUsers = userRepository.findAllOldHardDeleteUsers(threeDaysAgo);

    for (User user : hardDeleteUsers) {
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

  public void announce(AnnouncementRequest announcementRequest) throws FirebaseMessagingException {
    fCMService.sendToTopic(FCMTopic.ADMIN_ALARM, announcementRequest.getTitle(),
        announcementRequest.getContent());
  }
}
