package com.moup.domain.user.application;

import com.moup.domain.auth.application.AuthService;
import com.moup.domain.auth.application.AuthServiceFactory;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
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

    /// 탈퇴 확정 처리. **소셜 연동 해제에 성공했을 때만** 유저를 삭제한다.
    ///
    /// 실패했는데도 삭제하면 재시도 근거(`social_tokens`)가 CASCADE로 함께 사라져
    /// 소셜 연동이 영구히 남는다. 삭제를 보류하면 유저가 `is_deleted = 1`로 남아
    /// 다음 배치가 다시 집어 재시도한다.
    @Async("taskExecutor")
    public void processUserDeletion(User user) {
        Login provider = user.getProvider();
        AuthService authService = authServiceFactory.getService(provider);

        // 해당 공급자의 서비스가 없으면 재시도해도 소용없는 영구 실패다. 삭제는 진행한다.
        if (authService == null) {
            log.error("소셜 연동 해제 불가 - 지원하지 않는 공급자입니다. 삭제를 진행합니다. userId={}, provider={}",
                    user.getId(), provider);
            userService.deleteUserHardlyByUserId(user.getId());
            return;
        }

        try {
            // 비동기 스레드 내부에서 동기적으로 revokeToken 호출 (레이스 컨디션 방지)
            authService.revokeToken(user.getId());
        } catch (AuthException e) {
            log.error("소셜 연동 해제 실패 - 삭제를 보류하고 다음 배치에서 재시도합니다. userId={}, provider={}, error={}",
                    user.getId(), provider, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("소셜 연동 해제 중 예상치 못한 오류 - 삭제를 보류합니다. userId={}, provider={}",
                    user.getId(), provider, e);
            return;
        }

        userService.deleteUserHardlyByUserId(user.getId());
    }
}
