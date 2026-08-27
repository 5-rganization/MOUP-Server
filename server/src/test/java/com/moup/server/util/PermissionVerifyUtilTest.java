package com.moup.server.util;

import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// 스코프 7 C2 회귀 테스트.
/// 탈퇴로 `owner_id` / `user_id`가 NULL이 된 행에서 NPE(500)가 아니라
/// 403(`InvalidPermissionAccessException`)이 나와야 한다.
class PermissionVerifyUtilTest {

    private final PermissionVerifyUtil util = new PermissionVerifyUtil();

    @Test
    @DisplayName("사장님이 탈퇴해 owner_id가 NULL이면 NPE가 아니라 403")
    void verifyOwnerPermission_nullOwner_throws403() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyOwnerPermission(1L, null));
    }

    @Test
    @DisplayName("owner_id NULL + worker user_id NULL이어도 NPE가 아니라 403")
    void verifyWorkerPermission_bothNull_throws403() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(1L, null, null));
    }

    @Test
    @DisplayName("owner_id만 NULL이고 요청자가 해당 근무자면 통과")
    void verifyWorkerPermission_nullOwnerButIsWorker_passes() {
        assertDoesNotThrow(() -> util.verifyWorkerPermission(1L, 1L, null));
    }

    @Test
    @DisplayName("worker user_id만 NULL이고 요청자가 사장님이면 통과")
    void verifyWorkerPermission_nullWorkerButIsOwner_passes() {
        assertDoesNotThrow(() -> util.verifyWorkerPermission(2L, null, 2L));
    }

    @Test
    @DisplayName("정상 케이스 — 본인이면 통과, 남이면 403")
    void normalCases() {
        assertDoesNotThrow(() -> util.verifyOwnerPermission(5L, 5L));
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyOwnerPermission(5L, 6L));
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(9L, 1L, 2L));
    }
}
