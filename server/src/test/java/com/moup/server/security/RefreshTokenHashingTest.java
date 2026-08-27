package com.moup.server.security;

import com.moup.global.security.token.UserToken;
import com.moup.global.security.token.UserTokenRepository;
import com.moup.global.security.token.UserTokenService;
import com.moup.global.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Phase 8 회귀 테스트 — refresh token을 평문으로 저장하지 않는다 (스코프 1 I7).
///
/// 이 토큰은 유효기간 7일짜리 전권 크리덴셜이다. 평문으로 두면
/// **DB 읽기 권한만으로 전 사용자 계정에 로그인할 수 있다.**
@ExtendWith(MockitoExtension.class)
class RefreshTokenHashingTest {

    private static final Long USER_ID = 1L;
    private static final String RAW_TOKEN = "eyJhbGciOiJIUzI1NiJ9.header.payload.signature";

    @Mock private JwtUtil jwtUtil;
    @Mock private UserTokenRepository userTokenRepository;
    @InjectMocks private UserTokenService userTokenService;

    @Test
    @DisplayName("저장되는 값은 원본 토큰이 아니다")
    void 평문으로_저장하지_않는다() {
        when(jwtUtil.getUserId(RAW_TOKEN)).thenReturn(USER_ID);
        when(userTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        userTokenService.saveOrUpdateToken(RAW_TOKEN, 604_800_000L);

        ArgumentCaptor<UserToken> saved = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(saved.capture());

        String stored = saved.getValue().getRefreshToken();
        assertNotEquals(RAW_TOKEN, stored, "원본 토큰이 그대로 저장되면 안 된다");
        assertFalse(stored.contains("signature"), "토큰 조각이 남아 있으면 안 된다");
    }

    @Test
    @DisplayName("해시로 저장해도 검증은 그대로 통과한다")
    void 해시_저장_후에도_검증된다() {
        when(jwtUtil.isValidRefreshTokenType(RAW_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserId(RAW_TOKEN)).thenReturn(USER_ID);
        when(userTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // 저장 경로가 만든 값을 그대로 검증 경로에 먹인다 (양쪽 해시 규칙이 같아야 통과).
        userTokenService.saveOrUpdateToken(RAW_TOKEN, 604_800_000L);
        ArgumentCaptor<UserToken> saved = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(saved.capture());

        when(userTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(UserToken.builder()
                .userId(USER_ID)
                .refreshToken(saved.getValue().getRefreshToken())
                .expiryDate(LocalDateTime.now(SEOUL_ZONE_ID).plusDays(7))
                .build()));

        assertTrue(userTokenService.isValidRefreshToken(RAW_TOKEN));
    }

    @Test
    @DisplayName("다른 토큰은 통과하지 못한다")
    void 다른_토큰은_거부된다() {
        when(jwtUtil.isValidRefreshTokenType("other")).thenReturn(true);
        when(jwtUtil.getUserId("other")).thenReturn(USER_ID);
        when(userTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(UserToken.builder()
                .userId(USER_ID)
                .refreshToken("0".repeat(64))   // 다른 해시
                .expiryDate(LocalDateTime.now(SEOUL_ZONE_ID).plusDays(7))
                .build()));

        assertFalse(userTokenService.isValidRefreshToken("other"));
    }
}
