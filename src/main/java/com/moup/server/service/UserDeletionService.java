package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UserService userService;
    private final AuthServiceFactory authServiceFactory;

    @Async("taskExecutor")
    public void processUserDeletion(User user) {
        Login provider = user.getProvider();
        AuthService authService = authServiceFactory.getService(provider);

        try {
            // 비동기 스레드 내부에서 동기적으로 revokeToken 호출 (레이스 컨디션 방지)
            authService.revokeToken(user.getId());
            // TODO: revokeToken 실패 내역을 별도 테이블에 기록하는 등의 후처리
        } catch (AuthException e) {
            log.error("Auth revoke failed for user: {}. Error: {}", user.getId(), e.getMessage());
        }  catch (Exception e) {
            log.error("Unexpected error while deleting user: {}. Error: {}", user.getId(), e.getMessage());
        } finally {
            // revokeToken 성공 여부와 관계 없이, DB에서 유저를 삭제
            userService.deleteUserHardlyByUserId(user.getId());
        }
    }
}