package com.moup.server.service;

import com.moup.global.infra.fcm.FCMService;
import com.moup.domain.user.application.WorkerService;
import com.moup.domain.alarm.domain.AlarmContent;
import com.moup.domain.alarm.domain.AlarmTitle;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.util.PermissionVerifyUtil;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WorkerServiceTest {

  @Mock // 1. 가짜 의존성 선언
  private WorkplaceRepository workplaceRepository;

  @Mock // 2. 가짜 의존성 선언
  private WorkerRepository workerRepository;

  @Mock // 3. 가짜 의존성 선언
  private PermissionVerifyUtil permissionVerifyUtil;

  @Mock // 4. 가짜 의존성 선언
  private FCMService fCMService;

  @InjectMocks // 가짜 의존성들을 이 클래스에 주입
  private WorkerService workerService; // 테스트 대상 클래스

  @Test
  @DisplayName("근무자 참여 승인 - 성공")
  void acceptWorker_Success() {
    // given (테스트 데이터 및 Mock 객체 행동 정의)
    Long ownerUserId = 1L;
    Long workplaceId = 10L;
    Long workerId = 100L;
    Long workerUserId = 2L;
    String workplaceName = "테스트 근무지";

    // 1. 가짜 Workplace 객체 생성
    Workplace mockWorkplace = Workplace.builder()
        .id(workplaceId)
        .ownerId(ownerUserId)
        .workplaceName(workplaceName)
        .build();

    // 2. 가짜 Worker 객체 생성
    Worker mockWorker = Worker.builder()
        .id(workerId)
        .userId(workerUserId)
        .workplaceId(workplaceId)
        .build();

    // 3. Mock 객체 행동 정의
    // 3-1. workplaceRepository.findById(workplaceId)가 호출되면, mockWorkplace를 포함한 Optional 반환
    when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(mockWorkplace));

    // 3-2. permissionVerifyUtil.verifyOwnerPermission()은 아무것도 하지 않음 (void 메서드)
    doNothing().when(permissionVerifyUtil).verifyOwnerPermission(ownerUserId, ownerUserId);

    // 3-3. workerRepository.findByIdAndWorkplaceId(...)가 호출되면, mockWorker를 포함한 Optional 반환
    when(workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)).thenReturn(Optional.of(mockWorker));

    // fCMService.sendToSingleUser는 void mock이라 기본이 no-op이다. 스텁이 필요 없다.
    // (예전 코드의 doNothing().when(...)은 매처와 생 null을 섞어 InvalidUseOfMatchersException을 냈고,
    //  그 미완성 스텁이 다음 테스트로 새어 UnfinishedStubbingException까지 일으켰다.)

    // when (테스트할 메서드 실제 호출)
    workerService.acceptWorker(ownerUserId, workplaceId, workerId);

    // then (결과 검증)

    // 1. workplaceRepository.findById가 1번 호출되었는지 검증
    verify(workplaceRepository, times(1)).findById(workplaceId);

    // 2. permissionVerifyUtil.verifyOwnerPermission이 1번 호출되었는지 검증
    verify(permissionVerifyUtil, times(1)).verifyOwnerPermission(ownerUserId, ownerUserId);

    // 3. fCMService.sendToSingleUser가 1번 호출되었는지 검증
    //    (정확한 알림 메시지 내용까지 검증)
    String expectedTitle = AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED.getTitle();
    String expectedContent = AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_ACCEPTED.getContent(workplaceName);
    verify(fCMService, times(1)).sendToSingleUser(ownerUserId, workerUserId, expectedTitle, expectedContent, null);

    // 4. workerRepository.updateIsAccepted가 1번 호출되었는지 검증 (가장 중요)
    verify(workerRepository, times(1)).updateIsAccepted(workerId, workerUserId, workplaceId, true);
  }

  @Test
  @DisplayName("근무자 참여 승인 - 실패 (근무지 없음)")
  void acceptWorker_Fail_WorkplaceNotFound() {
    // given
    Long ownerUserId = 1L;
    Long workplaceId = 10L;
    Long workerId = 100L;

    // workplaceRepository.findById()가 빈 Optional을 반환하도록 설정
    when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.empty());

    // when & then
    // WorkplaceNotFoundException 예외가 발생하는지 검증
    assertThrows(WorkplaceNotFoundException.class, () -> {
      workerService.acceptWorker(ownerUserId, workplaceId, workerId);
    });

    // 예외가 발생했으므로 updateIsAccepted는 절대 호출되면 안 됨
    verify(workerRepository, never()).updateIsAccepted(anyLong(), anyLong(), anyLong(), anyBoolean());
  }

  @Test
  @DisplayName("승인은 푸시보다 먼저 확정된다 — 푸시 실패가 승인을 되돌리지 않는다")
  void acceptWorker_ConfirmsBeforePush() {
    // given
    Long ownerUserId = 1L;
    Long workplaceId = 10L;
    Long workerId = 100L;
    Long workerUserId = 2L;
    String workplaceName = "테스트 근무지";

    Workplace mockWorkplace = Workplace.builder().id(workplaceId).ownerId(ownerUserId).workplaceName(workplaceName).build();
    Worker mockWorker = Worker.builder().id(workerId).userId(workerUserId).workplaceId(workplaceId).build();

    when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(mockWorkplace));
    doNothing().when(permissionVerifyUtil).verifyOwnerPermission(ownerUserId, ownerUserId);
    when(workerRepository.findByIdAndWorkplaceId(workerId, workplaceId)).thenReturn(Optional.of(mockWorker));

    // when
    workerService.acceptWorker(ownerUserId, workplaceId, workerId);

    // then — 순서가 핵심이다.
    //
    // 예전에는 푸시를 **먼저** 보내고 실패 시 예외를 다시 던져 트랜잭션을 롤백시켰다.
    // 알바생이 앱을 지워 토큰이 죽으면(UNREGISTERED) 사장님이 승인을 누를 때마다 500이 났고,
    // 죽은 토큰을 정리하는 코드가 없어 **영구히 승인 불가**였다.
    //
    // 이제 승인을 먼저 확정하고 푸시는 커밋 이후 best-effort로 나간다.
    InOrder inOrder = inOrder(workerRepository, fCMService);
    inOrder.verify(workerRepository).updateIsAccepted(workerId, workerUserId, workplaceId, true);
    inOrder.verify(fCMService).sendToSingleUser(eq(ownerUserId), eq(workerUserId), anyString(), anyString(), isNull());
  }

}
