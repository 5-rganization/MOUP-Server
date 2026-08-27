package com.moup.server.service;

import com.moup.domain.alarm.mapper.AlarmRepository;
import com.moup.domain.routine.mapper.RoutineRepository;
import com.moup.domain.user.application.UserService;
import com.moup.domain.user.domain.User;
import com.moup.global.error.AlreadyDeletedException;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.infra.file.FileService;
import com.moup.global.infra.fcm.FCMTokenService;
import com.moup.global.infra.s3.S3Service;
import com.moup.global.security.token.SocialTokenRepository;
import com.moup.global.security.token.SocialTokenService;
import com.moup.global.security.token.UserTokenService;
import com.moup.global.util.JwtUtil;
import com.moup.global.util.NameVerifyUtil;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// 탈퇴 확정을 하드 삭제에서 **가명처리**로 바꾼 것에 대한 회귀 테스트 (확정 정책 5·7).
///
/// 가장 위험한 실수는 조용하다: 하드 삭제를 없애면 `users`의 CASCADE가 더 이상
/// 발화하지 않으므로, 지워야 할 것을 직접 지워야 한다. 하나라도 빠뜨리면
/// **탈퇴자의 소셜 자격증명·refresh 토큰·푸시 토큰이 DB에 그대로 남는다.**
/// 아무 증상도 없어서 들여다보기 전에는 알 수 없다.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserAnonymizationTest {

  private static final Long USER_ID = 1L;

  @Mock private FileService fileService;
  @Mock private S3Service s3Service;
  @Mock private SocialTokenService socialTokenService;
  @Mock private UserTokenService userTokenService;
  @Mock private UserRepository userRepository;
  @Mock private AlarmRepository alarmRepository;
  @Mock private RoutineRepository routineRepository;
  @Mock private SocialTokenRepository socialTokenRepository;
  @Mock private NameVerifyUtil nameVerifyUtil;
  @Mock private JwtUtil jwtUtil;
  @Mock private FCMTokenService fcmTokenService;

  @InjectMocks private UserService userService;

  private void givenUser(String profileImg, String anonymizedAt) {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
        User.builder().id(USER_ID).nickname("홍길동").profileImg(profileImg)
            .isDeleted(true).anonymizedAt(anonymizedAt).build()));
  }

  @Test
  @DisplayName("CASCADE로 지워지던 개인 데이터를 전부 직접 지운다")
  void 의존_테이블을_직접_지운다() {
    givenUser(null, null);

    userService.anonymizeUserByUserId(USER_ID);

    // 이 여섯 가지가 예전에는 users 하드 삭제의 CASCADE로 사라졌다.
    verify(socialTokenRepository).deleteByUserId(USER_ID);          // 소셜 자격증명
    verify(userTokenService).deleteToken(USER_ID);                  // refresh 토큰
    verify(fcmTokenService).deleteAllUserFCMTokens(USER_ID);        // 푸시 토큰 = 기기 식별자
    verify(routineRepository).deleteAllByUserId(USER_ID);           // routine_tasks는 CASCADE
    verify(alarmRepository).deleteAllNormalAlarmByUserId(USER_ID);
    verify(alarmRepository).deleteAllAdminAlarmMappingsByUserId(USER_ID);
  }

  @Test
  @DisplayName("users 행은 지우지 않고 가명처리한다")
  void 행을_지우지_않는다() {
    givenUser(null, null);

    userService.anonymizeUserByUserId(USER_ID);

    verify(userRepository).anonymizeUserById(USER_ID);
    // `hardDeleteUserById`는 저장소에서 아예 제거했다 — 되살리면 컴파일이 깨진다.
    // 하드 삭제하면 workplaces·workers·works·salaries가 함께 무너진다.
  }

  @Test
  @DisplayName("S3 프로필 이미지를 지운다 — 예전에는 객체가 영구히 남았다")
  void 프로필_이미지를_지운다() {
    givenUser("https://s3/profile.jpg", null);
    when(s3Service.doesFileExist("https://s3/profile.jpg")).thenReturn(true);

    userService.anonymizeUserByUserId(USER_ID);

    verify(s3Service).deleteFile("https://s3/profile.jpg");
  }

  @Test
  @DisplayName("S3 삭제가 실패해도 탈퇴 처리는 완료된다")
  void S3_실패가_탈퇴를_막지_않는다() {
    givenUser("https://s3/profile.jpg", null);
    when(s3Service.doesFileExist(anyString())).thenThrow(new RuntimeException("S3 down"));

    assertDoesNotThrow(() -> userService.anonymizeUserByUserId(USER_ID));
    // 고아 파일 하나가 남는 것이, 탈퇴가 영원히 안 되는 것보다 낫다.
    verify(userRepository).anonymizeUserById(USER_ID);
  }

  @Test
  @DisplayName("가명처리된 계정은 복구되지 않는다")
  void 가명처리_후_복구_불가() {
    givenUser(null, "2026-08-28T04:00:00");

    assertThrows(AlreadyDeletedException.class, () -> userService.restoreUserByUserId(USER_ID));
    // is_deleted만 0으로 돌아가면 이름도 소셜 연동도 없는 유령 계정이 살아난다.
    verify(userRepository, never()).undeleteUserById(anyLong());
  }

  // ---------------------------------------------------------------
  // 배치 재처리 방지 — SQL 문자열 수준에서 확인한다
  // ---------------------------------------------------------------

  /// 하드 삭제를 없앴으므로 `is_deleted = 1` 행이 영구히 남는다.
  /// 배치 쿼리가 `anonymized_at IS NULL`로 거르지 않으면 이미 처리한 사용자를
  /// **매일 다시 집어** 소셜 연동 해제를 무한 재시도한다. 조용히 반복되므로
  /// 로그를 보지 않으면 알 수 없다.
  @Test
  @DisplayName("탈퇴 배치 조회 3개가 이미 처리된 사용자를 제외한다")
  void 배치가_처리완료를_다시_집지_않는다() {
    Configuration configuration = new Configuration();
    configuration.addMapper(UserRepository.class);

    for (String method : new String[]{
        "findAllHardDeleteUsers", "findAllOldHardDeleteUsers", "findAllRevokeGiveUpUsers"}) {
      String sql = configuration
          .getMappedStatement(UserRepository.class.getName() + "." + method)
          .getBoundSql(null).getSql().replaceAll("\\s+", " ");
      assertTrue(sql.contains("anonymized_at IS NULL"),
          method + "이 이미 가명처리된 사용자를 다시 집는다: " + sql);
    }
  }
}
