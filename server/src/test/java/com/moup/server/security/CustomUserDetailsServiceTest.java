package com.moup.server.security;

import com.moup.domain.user.domain.User;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.common.type.Role;
import com.moup.global.security.CustomUserDetailsService;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/// 인증 경계가 계정의 삭제 상태를 확인하는지 검증한다 (스코프 1 C2 / 스코프 4 I1).
///
/// 수정 전에는 `is_deleted` 검사가 없어 탈퇴 신청한 유저가 유예기간 내내 정상 인증되었고,
/// `domain/workplace`·`routine`·`alarm` 어디에도 재확인이 없어 근무지·근무 데이터를 계속
/// 변경할 수 있었다. 더 나쁘게는, 계정 탈취를 당했을 때 "탈퇴"라는 자구책이 무력화되었다.
///
/// 유예기간 복구는 `/auth/login`(permitAll)에서 소셜 재인증으로 이루어지므로 이 차단에
/// 영향받지 않는다.
@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

  private static final Long USER_ID = 7L;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomUserDetailsService customUserDetailsService;

  @Test
  @DisplayName("정상 유저는 인증된다")
  void 정상_유저_인증() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(false)));

    var details = customUserDetailsService.loadUserByUsername(String.valueOf(USER_ID));

    assertNotNull(details);
    assertEquals("ROLE_WORKER", details.getAuthorities().iterator().next().getAuthority());
  }

  @Test
  @DisplayName("[C2] 탈퇴 신청한 유저는 인증 경계에서 차단된다")
  void 탈퇴_신청_유저_차단() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(true)));

    assertThrows(UsernameNotFoundException.class,
        () -> customUserDetailsService.loadUserByUsername(String.valueOf(USER_ID)),
        "is_deleted = 1 인 유저는 인증되면 안 된다");
  }

  @Test
  @DisplayName("숫자가 아닌 식별자는 거부된다")
  void 잘못된_식별자() {
    assertThrows(UsernameNotFoundException.class,
        () -> customUserDetailsService.loadUserByUsername("not-a-number"));
  }

  private static User user(boolean deleted) {
    return User.builder()
        .id(USER_ID).username("tester").nickname("테스터")
        .role(Role.ROLE_WORKER).isDeleted(deleted)
        .build();
  }
}
