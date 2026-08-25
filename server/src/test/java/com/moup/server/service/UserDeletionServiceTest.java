package com.moup.server.service;

import com.moup.domain.auth.application.AuthService;
import com.moup.domain.auth.application.AuthServiceFactory;
import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.application.UserDeletionService;
import com.moup.domain.user.application.UserService;
import com.moup.domain.user.domain.User;

import jakarta.security.auth.message.AuthException;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// 소셜 연동 해제(revoke)에 성공했을 때만 유저를 삭제하는지 검증한다.
///
/// 수정 전에는 `finally` 블록에서 성공 여부와 무관하게 하드 삭제를 했다. 그 결과
/// revoke가 실패해도 유저가 사라지고, 재시도 근거인 `social_tokens`도 `ON DELETE CASCADE`로
/// 함께 소멸해 **소셜 연동이 영구히 남았다.** 사용자는 탈퇴했다고 믿지만 Google/Apple
/// 계정 설정에는 앱 연동이 그대로 남는다.
@ExtendWith(MockitoExtension.class)
public class UserDeletionServiceTest {

  private static final Long USER_ID = 11L;

  @Mock
  private UserService userService;

  @Mock
  private AuthServiceFactory authServiceFactory;

  @Mock
  private AuthService authService;

  @InjectMocks
  private UserDeletionService userDeletionService;

  @Test
  @DisplayName("revoke에 성공하면 유저를 삭제한다")
  void revoke_성공시_삭제() throws Exception {
    when(authServiceFactory.getService(Login.LOGIN_GOOGLE)).thenReturn(authService);

    userDeletionService.processUserDeletion(user());

    verify(authService, times(1)).revokeToken(USER_ID);
    verify(userService, times(1)).deleteUserHardlyByUserId(USER_ID);
  }

  @Test
  @DisplayName("revoke가 실패하면 삭제를 보류한다 (다음 배치에서 재시도)")
  void revoke_실패시_삭제_보류() throws Exception {
    when(authServiceFactory.getService(Login.LOGIN_GOOGLE)).thenReturn(authService);
    doThrow(new AuthException("소셜 리프레시 토큰이 없습니다."))
        .when(authService).revokeToken(USER_ID);

    userDeletionService.processUserDeletion(user());

    verify(userService, never()).deleteUserHardlyByUserId(anyLong());
  }

  @Test
  @DisplayName("네트워크 오류로 revoke가 실패해도 삭제를 보류한다")
  void 네트워크_오류시_삭제_보류() throws Exception {
    when(authServiceFactory.getService(Login.LOGIN_GOOGLE)).thenReturn(authService);
    doThrow(new IOException("connection reset"))
        .when(authService).revokeToken(USER_ID);

    userDeletionService.processUserDeletion(user());

    verify(userService, never()).deleteUserHardlyByUserId(anyLong());
  }

  @Test
  @DisplayName("지원하지 않는 공급자는 영구 실패이므로 삭제를 진행한다")
  void 공급자_없으면_삭제_진행() {
    when(authServiceFactory.getService(Login.LOGIN_GOOGLE)).thenReturn(null);

    userDeletionService.processUserDeletion(user());

    verify(userService, times(1)).deleteUserHardlyByUserId(USER_ID);
  }

  private static User user() {
    return User.builder().id(USER_ID).provider(Login.LOGIN_GOOGLE).nickname("테스터").build();
  }
}
