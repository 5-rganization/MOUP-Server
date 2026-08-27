package com.moup.server.util;

import com.moup.domain.user.domain.Worker;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

/// Phase 2 회귀 테스트 — `is_accepted` 승인 게이트.
///
/// 이전에는 `is_accepted`를 `WHERE`절이나 조건문으로 읽는 곳이 코드베이스 전체에 **0건**이었다.
/// 초대코드를 손에 넣은 사람은 참여 요청 201을 받는 순간 승인된 근무자와 동일한 권한을 가졌고,
/// 사장님의 "승인" 버튼은 아무것도 막지 못했다.
class IsAcceptedGateTest {

    private static final Long WORKER_USER_ID = 1L;
    private static final Long OWNER_USER_ID = 2L;
    private static final Long STRANGER_USER_ID = 99L;

    // 승인 게이트만 본다 — 소유자 탈퇴 조회는 일어나지 않는다.
    private final PermissionVerifyUtil util = new PermissionVerifyUtil(mock(UserRepository.class));

    private Worker worker(Boolean accepted) {
        return Worker.builder().id(10L).userId(WORKER_USER_ID).workplaceId(20L).isAccepted(accepted).build();
    }

    @Test
    @DisplayName("승인 대기 중인 근무자는 근무 행위가 차단된다")
    void pendingWorkerIsBlocked() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(WORKER_USER_ID, worker(false), OWNER_USER_ID));
    }

    @Test
    @DisplayName("is_accepted가 NULL이어도 차단한다 (fail-closed)")
    void nullAcceptedIsBlocked() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(WORKER_USER_ID, worker(null), OWNER_USER_ID));
    }

    @Test
    @DisplayName("승인된 근무자는 통과한다")
    void acceptedWorkerPasses() {
        assertDoesNotThrow(() -> util.verifyWorkerPermission(WORKER_USER_ID, worker(true), OWNER_USER_ID));
    }

    @Test
    @DisplayName("사장님은 미승인 근무자의 근무도 다룰 수 있다")
    void ownerPassesRegardlessOfApproval() {
        assertDoesNotThrow(() -> util.verifyWorkerPermission(OWNER_USER_ID, worker(false), OWNER_USER_ID));
    }

    @Test
    @DisplayName("남의 근무는 승인 여부와 무관하게 차단된다")
    void strangerIsBlocked() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(STRANGER_USER_ID, worker(true), OWNER_USER_ID));
    }

    @Test
    @DisplayName("참여 취소와 자기 급여 설정은 승인 대기 중에도 허용된다 (정책 6 (b))")
    void pendingWorkerCanStillManageOwnRecord() {
        assertDoesNotThrow(() -> util.verifyWorkerIdentityAllowingPending(
                WORKER_USER_ID, WORKER_USER_ID, OWNER_USER_ID));
    }

    @Test
    @DisplayName("owner_id가 NULL(사장님 탈퇴)이어도 NPE가 아니라 403")
    void nullOwnerStillFailsClosed() {
        assertThrows(InvalidPermissionAccessException.class,
                () -> util.verifyWorkerPermission(STRANGER_USER_ID, worker(true), null));
    }
}
