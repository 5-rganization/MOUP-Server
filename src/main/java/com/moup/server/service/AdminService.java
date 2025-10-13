package com.moup.server.service;

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

    @Value("${user.delete.grace-period}")
    private int gracePeriod;

    public void hardDeleteOldUsers(Boolean isImmediate) {
        // 하드 삭제 대상 유저 목록 조회
        List<User> hardDeleteUsers;
        if (Boolean.TRUE.equals(isImmediate)) {
            hardDeleteUsers = userRepository.findAllHardDeleteUsers();
        } else {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(gracePeriod);
            hardDeleteUsers = userRepository.findAllOldHardDeleteUsers(threeDaysAgo);
        }
        
        for (User user : hardDeleteUsers) {
            userDeletionService.processUserDeletion(user);
        }
    }
}
