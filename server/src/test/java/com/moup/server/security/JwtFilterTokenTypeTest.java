package com.moup.server.security;

import com.moup.domain.user.domain.User;
import com.moup.global.common.type.Role;
import com.moup.global.security.CustomUserDetails;
import com.moup.global.security.CustomUserDetailsService;
import com.moup.global.security.JwtFilter;
import com.moup.global.security.token.TokenCreateRequest;
import com.moup.global.util.JwtUtil;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// `JwtFilter`가 access token과 refresh token을 구분하는지 검증한다.
///
/// 코드 리뷰(스코프 1 C1)에서 제기된 주장을 **추론이 아니라 실제 실행으로** 확인하기 위한
/// 테스트다. 실제 `JwtUtil`로 토큰을 만들어 실제 `JwtFilter`에 통과시키고,
/// `SecurityContext`에 인증 객체가 실리는지 본다.
///
/// C1 수정(`typ` 클레임 + `isValidAccessToken` 가드) 이후의 기대 동작을 고정한다.
/// 수정 전에는 `refresh token 거부` 테스트가 실패했다 — 실제로 인증이 통과했다.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JwtFilterTokenTypeTest {

  /// 운영과 무관한 테스트 전용 키. HS256 최소 길이(256bit = 32byte)를 넘겨야 한다.
  private static final String TEST_SECRET = "moup-test-secret-key-for-jwt-filter-verification-0123456789";
  private static final long ACCESS_EXPIRATION = 1_200_000L;    // 20분 (운영값)
  private static final long REFRESH_EXPIRATION = 604_800_000L; // 7일 (운영값)
  private static final Long USER_ID = 42L;

  private JwtUtil jwtUtil;
  private JwtFilter jwtFilter;

  @Mock
  private CustomUserDetailsService customUserDetailsService;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil(TEST_SECRET);
    ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", ACCESS_EXPIRATION);
    ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", REFRESH_EXPIRATION);

    jwtFilter = new JwtFilter(jwtUtil, customUserDetailsService);

    User user = User.builder().id(USER_ID).username("tester").nickname("테스터").role(Role.ROLE_OWNER).build();
    when(customUserDetailsService.loadUserByUsername(anyString()))
        .thenReturn(new CustomUserDetails(user));

    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("access token으로 인증이 통과한다 (정상 동작)")
  void access_token_인증_통과() throws Exception {
    Authentication authentication = runFilterWith(jwtUtil.createAccessToken(tokenRequest()));

    assertNotNull(authentication, "access token은 인증되어야 한다");
    assertEquals("ROLE_OWNER",
        authentication.getAuthorities().iterator().next().getAuthority(),
        "권한은 토큰이 아니라 DB에서 온다");
  }

  /// **C1 회귀 방지 — 이 테스트가 이번 리뷰에서 가장 중요하다.**
  ///
  /// 수정 전에는 `createRefreshToken`이 `subject`만 담고 타입 구분이 없었고
  /// `JwtFilter`가 서명·만료만 검사해, refresh token이 access token과 완전히
  /// 동일하게 동작했다(7일짜리 전권 크리덴셜).
  @Test
  @DisplayName("[C1] refresh token은 인증에 사용할 수 없다")
  void refresh_token은_인증에_사용할_수_없다() throws Exception {
    Authentication authentication = runFilterWith(jwtUtil.createRefreshToken(tokenRequest()));

    assertNull(authentication,
        "refresh token은 /auth/token/refresh 전용이며 Bearer 인증에 쓰이면 안 된다");
  }

  /// `typ` 클레임이 없는 토큰(= 수정 이전에 발급된 토큰)도 거부되어야 한다.
  /// 이것이 거부되지 않으면 기존 refresh token으로 7일간 구멍이 유지된다.
  @Test
  @DisplayName("[C1] typ 클레임이 없는 구버전 토큰도 거부된다")
  void typ가_없는_구버전_토큰_거부() throws Exception {
    String legacyToken = io.jsonwebtoken.Jwts.builder()
        .subject(String.valueOf(USER_ID))
        .claim("role", Role.ROLE_OWNER.name())
        .issuedAt(new java.util.Date())
        .expiration(new java.util.Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
        .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        .compact();

    assertNull(runFilterWith(legacyToken),
        "typ 없는 토큰을 허용하면 기존 refresh token으로 구멍이 유지된다");
  }

  @Test
  @DisplayName("[C1] refresh token의 유효 기간이 access token의 504배다")
  void refresh_token_수명이_훨씬_길다() {
    assertEquals(504L, REFRESH_EXPIRATION / ACCESS_EXPIRATION,
        "access 20분 vs refresh 7일 — 유출 시 노출 창의 차이");
  }

  @Test
  @DisplayName("서명이 다른 토큰은 거부된다")
  void 서명이_다른_토큰_거부() throws Exception {
    JwtUtil attacker = new JwtUtil("attacker-forged-secret-key-0123456789-abcdefghijklmnop");
    ReflectionTestUtils.setField(attacker, "accessTokenExpiration", ACCESS_EXPIRATION);

    assertNull(runFilterWith(attacker.createAccessToken(tokenRequest())),
        "다른 키로 서명된 토큰은 통과하면 안 된다");
  }

  @Test
  @DisplayName("Bearer 헤더가 없으면 인증되지 않는다")
  void 헤더_없음() throws Exception {
    assertNull(runFilterWith(null), "헤더가 없으면 미인증으로 진행한다");
  }

  // ---------------------------------------------------------------

  /// 필터를 실제로 실행하고 `SecurityContext`에 실린 인증 객체를 돌려준다.
  private Authentication runFilterWith(String token) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (token != null) {
      request.addHeader("Authorization", "Bearer " + token);
    }
    jwtFilter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static TokenCreateRequest tokenRequest() {
    return TokenCreateRequest.builder()
        .userId(USER_ID)
        .role(Role.ROLE_OWNER)
        .username("tester")
        .build();
  }
}
