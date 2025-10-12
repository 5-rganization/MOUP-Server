package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UserRepository userRepository;
    private final AuthServiceFactory authServiceFactory;

    private final TransactionTemplate transactionTemplate;

    @Async("taskExecutor")
    public void processUserDeletion(User user) {
        Login provider = user.getProvider();
        AuthService authService = authServiceFactory.getService(provider);

        try {
            // 동기적으로 revokeToken 호출 (결과를 기다림)
            authService.revokeToken(user.getId());

            transactionTemplate.executeWithoutResult(status -> {
                // revoke가 성공적으로 끝나면, DB에서 유저를 삭제
                userRepository.hardDeleteUserById(user.getId());
            });

        } catch (AuthException e) {
            log.error("Auth revoke failed for user: {}. Error: {}", user.getId(), e.getMessage());
        }  catch (Exception e) {
            log.error("Unexpected error while deleting user: {}. Error: {}", user.getId(), e.getMessage());
        }
        // TODO: 실패 내역을 별도 테이블에 기록하는 등의 후처리
    }
}