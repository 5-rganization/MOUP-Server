package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
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
    private final AuthServiceFactory authServiceFactory;

    @Value("${user.delete.grace-period}")
    private int gracePeriod;

    @Transactional
    public void hardDeleteOldUsers() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(gracePeriod);
        
        // 1. 하드 삭제 대상 유저 목록 조회
        List<User> hardDeleteUsers = userRepository.findAllHardDeleteUsers(threeDaysAgo);
        
        for (User user : hardDeleteUsers) {
            try {
                // 2. 유저의 소셜 로그인 타입 가져옴
                Login provider = user.getProvider();

                // 3. Factory를 통해 provider에 맞는 AuthService 구현체를 가져옴
                AuthService authService = authServiceFactory.getService(provider);

                // 4. 가져온 서비스의 revokeToken 메서드를 호출
                authService.revokeToken(user.getId());

                // 5. revoke 성공 시 DB에서 유저를 완전히 삭제
                userRepository.hardDeleteUserById(user.getId());
            } catch (Exception e) {
                // TODO: revoke 실패 시 로직 추가
                // 유저의 revoke 실패 flag 설정?
            }
        }
    }
}
