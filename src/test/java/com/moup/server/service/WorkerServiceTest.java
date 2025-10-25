package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.server.common.AlarmContent;
import com.moup.server.common.AlarmTitle;
import com.moup.server.exception.CustomFirebaseMessagingException;
import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.model.entity.Worker;
import com.moup.server.model.entity.Workplace;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.repository.WorkplaceRepository;
import com.moup.server.util.PermissionVerifyUtil;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  void acceptWorker_Success() throws FirebaseMessagingException {
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

    // 3-4. fCMService.sendToSingleUser(...)는 아무것도 하지 않음 (성공 시)
    doNothing().when(fCMService).sendToSingleUser(anyLong(), anyLong(), anyString(), anyString());

    // when (테스트할 메서드 실제 호출)
    workerService.acceptWorker(ownerUserId, workplaceId, workerId);

    // then (결과 검증)

    // 1. workplaceRepository.findById가 1번 호출되었는지 검증
    verify(workplaceRepository, times(1)).findById(workplaceId);

    // 2. permissionVerifyUtil.verifyOwnerPermission이 1번 호출되었는지 검증
    verify(permissionVerifyUtil, times(1)).verifyOwnerPermission(ownerUserId, ownerUserId);

    // 3. fCMService.sendToSingleUser가 1번 호출되었는지 검증
    //    (정확한 알림 메시지 내용까지 검증)
    String expectedTitle = AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED.toString();
    String expectedContent = AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_ACCEPTED.getContent(workplaceName);
    verify(fCMService, times(1)).sendToSingleUser(ownerUserId, workerUserId, expectedTitle, expectedContent);

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
  @DisplayName("근무자 참여 승인 - 실패 (FCM 전송 실패)")
  void acceptWorker_Fail_FCMSendError() throws FirebaseMessagingException {
    // given
    // (Success 케이스와 동일하게 mockWorkplace, mockWorker 설정 ...)
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

    // 3-4. fCMService.sendToSingleUser()가 호출되면 FirebaseMessagingException 예외를 던지도록 설정
    fCMService.sendToSingleUser(anyLong(), anyLong(), anyString(), anyString());

    // when & then
    // CustomFirebaseMessagingException 예외가 발생하는지 검증
    assertThrows(CustomFirebaseMessagingException.class, () -> {
      workerService.acceptWorker(ownerUserId, workplaceId, workerId);
    });

    // FCM 전송에 실패했으므로 updateIsAccepted는 절대 호출되면 안 됨
    verify(workerRepository, never()).updateIsAccepted(anyLong(), anyLong(), anyLong(), anyBoolean());
  }

}
