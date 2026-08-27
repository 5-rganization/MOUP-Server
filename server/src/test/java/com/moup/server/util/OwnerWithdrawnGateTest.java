package com.moup.server.util;

import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/// 확정 정책 5 — 사장님 탈퇴 시 **데이터 보존, 쓰기만 차단**.
///
/// 조회를 함께 막지 않는 것이 핵심이다. 근무·급여 기록은 사장님만의 것이 아니라
/// 알바생의 임금·소득 증빙이기도 하다. 데이터를 보존하는 목적이 그것인데
/// 정작 당사자가 못 보면 보존의 의미가 없다.
@ExtendWith(MockitoExtension.class)
public class OwnerWithdrawnGateTest {

  private static final Long OWNER_ID = 2L;

  @Mock private UserRepository userRepository;
  @InjectMocks private PermissionVerifyUtil util;

  @Test
  @DisplayName("사장님이 탈퇴했으면 쓰기를 막는다")
  void 탈퇴한_사장님_근무지는_쓰기_불가() {
    when(userRepository.isWithdrawn(OWNER_ID)).thenReturn(true);

    assertThrows(InvalidPermissionAccessException.class,
        () -> util.verifyWorkplaceIsWritable(OWNER_ID));
  }

  @Test
  @DisplayName("사장님이 정상이면 쓰기를 막지 않는다")
  void 정상_근무지는_쓰기_가능() {
    when(userRepository.isWithdrawn(OWNER_ID)).thenReturn(false);

    assertDoesNotThrow(() -> util.verifyWorkplaceIsWritable(OWNER_ID));
  }

  /// 과거 하드 삭제로 `owner_id`가 NULL이 된 근무지도 같은 취급이다.
  /// 조회조차 안 하고 막아야 한다 — `isWithdrawn(null)`은 의미 없는 쿼리다.
  @Test
  @DisplayName("owner_id가 NULL이면 조회 없이 막는다")
  void owner_id가_NULL이면_차단() {
    assertThrows(InvalidPermissionAccessException.class,
        () -> util.verifyWorkplaceIsWritable(null));
  }
}
