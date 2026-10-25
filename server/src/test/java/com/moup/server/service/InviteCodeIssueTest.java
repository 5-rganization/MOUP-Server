package com.moup.server.service;

import com.moup.domain.workplace.application.InviteCodeService;
import com.moup.domain.workplace.exception.InviteCodeGenerationException;
import com.moup.domain.workplace.mapper.InviteCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 초대 코드 발급 (스코프 5 I-5 · M-1 · M-2 · M-3 · M-5).
@ExtendWith(MockitoExtension.class)
class InviteCodeIssueTest {

    private static final Long WORKPLACE_ID = 10L;

    @Mock private InviteCodeRepository inviteCodeRepository;
    @InjectMocks private InviteCodeService inviteCodeService;

    @BeforeEach
    void setUp() {
        inviteCodeService.init();
        ReflectionTestUtils.setField(inviteCodeService, "inviteCodeTtlMinutes", 7);
    }

    /// 예전에는 존재 확인 뒤 따로 저장해, 같은 코드를 동시에 뽑은 두 근무지 중 나중 쪽이 앞 근무지의 매핑을 덮어썼다.
    @Test
    @DisplayName("다른 근무지가 이미 잡은 코드면 덮어쓰지 않고 다시 뽑는다")
    void 충돌하면_다시_뽑는다() {
        when(inviteCodeRepository.findInviteCodeByWorkplaceId(WORKPLACE_ID)).thenReturn(Optional.empty());
        when(inviteCodeRepository.claim(anyString(), eq(WORKPLACE_ID), any())).thenReturn(false, true);

        InviteCodeService.IssuedInviteCode issued = inviteCodeService.generateInviteCode(WORKPLACE_ID, false);

        assertFalse(issued.reused());
        verify(inviteCodeRepository, times(2)).claim(anyString(), eq(WORKPLACE_ID), eq(Duration.ofMinutes(7)));
    }

    /// "기존 코드인지"를 발급과 같은 조회로 정한다. 예전에는 따로 물어 그 사이 TTL이 끝나면 새 코드를 기존 코드라고 답했다.
    @Test
    @DisplayName("유효한 코드가 있으면 새로 만들지 않고 reused=true로 돌려준다")
    void 기존_코드_재사용() {
        when(inviteCodeRepository.findInviteCodeByWorkplaceId(WORKPLACE_ID)).thenReturn(Optional.of("MUP234"));

        InviteCodeService.IssuedInviteCode issued = inviteCodeService.generateInviteCode(WORKPLACE_ID, false);

        assertEquals("MUP234", issued.code());
        assertTrue(issued.reused());
        verify(inviteCodeRepository, never()).claim(any(), any(), any());
    }

    @Test
    @DisplayName("재발급이면 기존 코드를 지우고 새로 잡는다")
    void 재발급() {
        when(inviteCodeRepository.findInviteCodeByWorkplaceId(WORKPLACE_ID)).thenReturn(Optional.of("MUP234"));
        when(inviteCodeRepository.claim(anyString(), eq(WORKPLACE_ID), any())).thenReturn(true);

        InviteCodeService.IssuedInviteCode issued = inviteCodeService.generateInviteCode(WORKPLACE_ID, true);

        verify(inviteCodeRepository).delete("MUP234", WORKPLACE_ID);
        assertFalse(issued.reused());
    }

    @Test
    @DisplayName("끝내 못 뽑으면 일반 500이 아니라 다시 시도하라는 503 계열 예외를 던진다")
    void 실패는_전용_예외() {
        when(inviteCodeRepository.findInviteCodeByWorkplaceId(WORKPLACE_ID)).thenReturn(Optional.empty());
        when(inviteCodeRepository.claim(anyString(), eq(WORKPLACE_ID), any())).thenReturn(false);

        assertThrows(InviteCodeGenerationException.class,
                () -> inviteCodeService.generateInviteCode(WORKPLACE_ID, false));
    }

    @Test
    @DisplayName("근무지를 지우면 그 근무지의 코드도 지운다")
    void 근무지_삭제시_코드_삭제() {
        when(inviteCodeRepository.findInviteCodeByWorkplaceId(WORKPLACE_ID)).thenReturn(Optional.of("MUP234"));

        inviteCodeService.deleteByWorkplaceId(WORKPLACE_ID);

        verify(inviteCodeRepository).delete("MUP234", WORKPLACE_ID);
    }

    @Test
    @DisplayName("저장소: 코드 키를 SET NX로 잡지 못하면 역방향 매핑을 쓰지 않는다")
    @SuppressWarnings("unchecked")
    void 선점_실패시_아무것도_안_쓴다() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("inviteCode:MUP234"), eq("10"), any(Duration.class))).thenReturn(false);

        assertFalse(new InviteCodeRepository(redis).claim("MUP234", WORKPLACE_ID, Duration.ofMinutes(10)));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
