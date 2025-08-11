package com.moup.server.service;

import com.moup.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    @Value("${user.delete.grace-period}")
    private int gracePeriod;

    @Transactional
    public void hardDeleteOldUsers() {
        // 3일(72시간) 이상 지난 유저를 삭제
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(gracePeriod);
        userRepository.hardDeleteUsersOlderThan(threeDaysAgo);
    }
}
