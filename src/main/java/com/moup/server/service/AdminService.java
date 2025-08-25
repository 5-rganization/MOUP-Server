package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import jakarta.security.auth.message.AuthException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final AppleAuthService appleAuthService;

    @Value("${user.delete.grace-period}")
    private int gracePeriod;

    @Transactional
    public void hardDeleteOldUsers() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(gracePeriod);
        
        // 1. 하드 삭제 대상 유저 목록 조회
        List<User> hardDeleteUsers = userRepository.findAllHardDeleteUsers(threeDaysAgo);
        
        for (User user : hardDeleteUsers) {
            // 2. 애플 유저의 토큰 revoke 처리
            if(user.getProvider() == Login.LOGIN_APPLE) {
                try {
                    appleAuthService.revokeToken(user.getId());
                    // revoke 성공 시 DB에서 유저 하드 삭제
                    userRepository.hardDeleteUserById(user.getId());
                } catch (Exception e) {
                    // TODO: revoke 실패 시 로직 추가
                    // 유저의 revoke 실패 flag 설정?
                }
            } else {
                // 3. DB에서 유저 하드 삭제
                userRepository.hardDeleteUserById(user.getId());
            }
        }
    }
}
