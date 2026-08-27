package com.moup.server.service;

import com.moup.domain.workplace.application.InviteCodeService;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.workplace.mapper.InviteCodeRepository;
import com.moup.global.error.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Phase 7 회귀 테스트 — 초대코드 무차별 대입 방어.
///
/// 키스페이스는 32⁶ ≈ 10.7억이라 **특정 코드**를 노리는 공격은 원래 안전하다.
/// 문제는 **무표적 공격**이다 — 아무 코드나 맞히면 되므로 동시 유효 코드가 L개일 때
/// 기댓값이 2³⁰/L회로 줄어든다. 초당 2,000회 기준 L=500이면 약 18분이고,
/// 제품이 성장할수록 나빠진다.
@ExtendWith(MockitoExtension.class)
class InviteCodeRateLimitTest {

    private static final Long USER_ID = 1L;
    private static final int MAX_ATTEMPTS = 20;

    @Mock private InviteCodeRepository inviteCodeRepository;
    @InjectMocks private InviteCodeService inviteCodeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inviteCodeService, "maxFailedAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(inviteCodeService, "attemptWindowMinutes", 10);
    }

    @Test
    @DisplayName("실패가 한도에 도달하면 조회 자체를 막는다")
    void 한도_도달_시_429() {
        when(inviteCodeRepository.countFailedAttempts(USER_ID)).thenReturn((long) MAX_ATTEMPTS);

        assertThrows(TooManyRequestsException.class,
                () -> inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(USER_ID, "ABC234"));

        // Redis 조회조차 하지 않아야 한다. 404 오라클 자체가 정보다.
        verify(inviteCodeRepository, never()).findWorkplaceIdByInviteCode(anyString());
    }

    @Test
    @DisplayName("조회 실패는 카운터를 올리고 404를 그대로 던진다")
    void 실패는_카운트된다() {
        when(inviteCodeRepository.countFailedAttempts(USER_ID)).thenReturn(3L);
        when(inviteCodeRepository.findWorkplaceIdByInviteCode("ABC234")).thenReturn(Optional.empty());

        assertThrows(WorkplaceNotFoundException.class,
                () -> inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(USER_ID, "ABC234"));

        verify(inviteCodeRepository).recordFailedAttempt(USER_ID, 10);
    }

    @Test
    @DisplayName("성공한 조회는 카운터를 올리지 않고 오히려 초기화한다")
    void 성공은_카운터를_초기화한다() {
        when(inviteCodeRepository.countFailedAttempts(USER_ID)).thenReturn(5L);
        when(inviteCodeRepository.findWorkplaceIdByInviteCode("ABC234")).thenReturn(Optional.of(42L));

        Long result = inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(USER_ID, "ABC234");

        assertEquals(42L, result);
        verify(inviteCodeRepository, never()).recordFailedAttempt(anyLong(), anyInt());
        // 정상 사용자가 코드를 몇 번 잘못 입력한 뒤 성공하면 흔적을 남기지 않는다.
        verify(inviteCodeRepository).clearFailedAttempts(USER_ID);
    }

    @Test
    @DisplayName("한도 직전까지는 통과한다")
    void 한도_미만은_통과() {
        when(inviteCodeRepository.countFailedAttempts(USER_ID)).thenReturn((long) MAX_ATTEMPTS - 1);
        when(inviteCodeRepository.findWorkplaceIdByInviteCode("ABC234")).thenReturn(Optional.of(42L));

        assertEquals(42L, inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(USER_ID, "ABC234"));
    }
}
