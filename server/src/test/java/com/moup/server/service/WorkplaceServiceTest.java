package com.moup.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.server.common.AlarmContent;
import com.moup.server.common.AlarmTitle;
import com.moup.server.common.Role;
import com.moup.server.exception.CustomFirebaseMessagingException;
import com.moup.server.exception.WorkplaceLimitExceededException;
import com.moup.server.model.dto.OwnerWorkplaceCreateRequest;
import com.moup.server.model.dto.SalaryCreateRequest;
import com.moup.server.model.dto.WorkplaceJoinRequest;
import com.moup.server.model.entity.User;
import com.moup.server.model.entity.Workplace;
import com.moup.server.model.entity.Worker;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkplaceRepository;
import com.moup.server.repository.WorkerRepository;
import com.moup.server.util.PermissionVerifyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// (Assertions, Matchers, Mockito static imports)
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkplaceServiceTest {

  @Mock
  private WorkplaceRepository workplaceRepository;
  @Mock
  private WorkerRepository workerRepository;
  @Mock
  private SalaryRepository salaryRepository;
  @Mock
  private InviteCodeService inviteCodeService;
  @Mock
  private PermissionVerifyUtil permissionVerifyUtil;
  @Mock
  private FCMService fcmService;

  @InjectMocks
  private WorkplaceService workplaceService;

  private User mockOwner;
  private User mockWorkerUser;
  private static final int TEST_WORKPLACE_LIMIT = 5; // 테스트용 개수 제한

  @BeforeEach
  void setUp() {
    // @Value로 주입되는 필드 값을 테스트용으로 수동 설정
    ReflectionTestUtils.setField(workplaceService, "workplaceCreationLimit", TEST_WORKPLACE_LIMIT);

    mockOwner = User.builder()
        .id(1L)
        .username("사장님")
        .role(Role.ROLE_OWNER)
        .build();

    mockWorkerUser = User.builder()
        .id(2L)
        .username("알바생")
        .role(Role.ROLE_WORKER)
        .build();
  }

  // ========== Test for createWorkplaceAndWorkerHelper (via createWorkplace) ==========

  @Test
  @DisplayName("TODO 테스트: 근무지 생성 시 개수 제한 초과 (실패)")
  void createWorkplace_Fail_LimitExceeded() {
    // given
    // 1. OwnerWorkplaceCreateRequest를 Builder로 생성 (Setter 사용 X)
    OwnerWorkplaceCreateRequest request = OwnerWorkplaceCreateRequest.builder()
        .workplaceName("6번째 근무지")
        // ... request에 다른 필드가 있다면 .builder()에 추가 ...
        .build();
    Long ownerId = mockOwner.getId();

    // 1. 이름 중복 검사는 통과
    when(workplaceRepository.existsByOwnerIdAndWorkplaceName(ownerId, request.getWorkplaceName()))
        .thenReturn(false);

    // 2. 근무지 개수 카운트 시, 설정한 LIMIT 값(5)과 같거나 큰 값을 반환
    when(workplaceRepository.getOwnedWorkplaceCountByUserId(ownerId))
        .thenReturn(TEST_WORKPLACE_LIMIT);

    // when & then
    // WorkplaceLimitExceededException 예외가 발생하는지 검증
    assertThrows(WorkplaceLimitExceededException.class, () -> {
      workplaceService.createWorkplace(mockOwner, request);
    });

    // 예외가 발생했으므로, workplace나 worker가 생성되면 안 됨
    verify(workplaceRepository, never()).create(any(Workplace.class));
    verify(workerRepository, never()).create(any(Worker.class));
  }

  @Test
  @DisplayName("TODO 테스트 관련: 근무지 생성 개수 제한 미만 (성공)")
  void createWorkplace_Success_UnderLimit() {
    // given
    // 1. OwnerWorkplaceCreateRequest를 Builder로 생성 (Setter 사용 X)
    OwnerWorkplaceCreateRequest request = OwnerWorkplaceCreateRequest.builder()
        .workplaceName("1번째 근무지")
        .build();
    Long ownerId = mockOwner.getId();

    // 1. 이름 중복 통과
    when(workplaceRepository.existsByOwnerIdAndWorkplaceName(ownerId, request.getWorkplaceName()))
        .thenReturn(false);

    // 2. 근무지 개수가 LIMIT 미만
    when(workplaceRepository.getOwnedWorkplaceCountByUserId(ownerId))
        .thenReturn(TEST_WORKPLACE_LIMIT - 1);

    // 3. workplaceRepository.create()가 호출될 때, 생성된 workplace 객체에 ID를 설정하도록 시뮬레이션
    doAnswer(invocation -> {
      Workplace workplaceArg = invocation.getArgument(0);
      ReflectionTestUtils.setField(workplaceArg, "id", 100L); // 가짜 ID 설정
      return null;
    }).when(workplaceRepository).create(any(Workplace.class));

    // when
    workplaceService.createWorkplace(mockOwner, request);

    // then
    // workplace와 worker가 각각 1번씩 생성되었는지 검증
    verify(workplaceRepository, times(1)).create(any(Workplace.class));
    verify(workerRepository, times(1)).create(any(Worker.class));
  }


  // ========== Test for joinWorkplace (FCM Logic) ==========

  @Test
  @DisplayName("TODO 테스트: 근무지 참가 신청 - FCM 알림 발송 성공")
  void joinWorkplace_Success_FCMSend() throws FirebaseMessagingException {
    // given
    String inviteCode = "TESTCODE";
    Long workplaceId = 10L;
    Long ownerId = 1L; // 사장님 ID

    WorkplaceJoinRequest request = WorkplaceJoinRequest.builder()
        .inviteCode(inviteCode)
        .workerBasedLabelColor("#FFFFFF")
        // 2. SalaryCreateRequest를 Builder로 생성 (NoArgsConstructor 사용 X)
        .salaryCreateRequest(SalaryCreateRequest.builder().build())
        .build();

    // 1. 초대 코드로 workplaceId 찾기
    when(inviteCodeService.findWorkplaceIdByInviteCode(inviteCode.toUpperCase())).thenReturn(workplaceId);
    // 2. 근무지 존재 확인
    when(workplaceRepository.existsById(workplaceId)).thenReturn(true);
    // 3. 이미 가입된 유저가 아님
    when(workerRepository.existsByUserIdAndWorkplaceId(mockWorkerUser.getId(), workplaceId)).thenReturn(false);
    // 4. 근무지 사장님 ID 찾기
    when(workplaceRepository.findOwnerId(workplaceId)).thenReturn(ownerId);

    // 5. FCMService가 호출되면 아무것도 하지 않음 (성공 시뮬레이션)
    doNothing().when(fcmService).sendToSingleUser(anyLong(), anyLong(), anyString(), anyString(), null);

    // 6. workerRepository.create()가 호출될 때, worker 객체에 ID 설정 시뮬레이션
    doAnswer(invocation -> {
      Worker workerArg = invocation.getArgument(0);
      ReflectionTestUtils.setField(workerArg, "id", 200L); // 가짜 Worker ID
      return null;
    }).when(workerRepository).create(any(Worker.class));

    // when
    workplaceService.joinWorkplace(mockWorkerUser, request);

    // then
    // 1. FCM 전송이 정확히 1번 호출되었는지 검증
    ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

    verify(fcmService, times(1)).sendToSingleUser(
        eq(mockWorkerUser.getId()), // from (알바생)
        eq(ownerId),               // to (사장님)
        titleCaptor.capture(),     // title
        contentCaptor.capture(),    // content
        null
    );

    // 2. 전송된 알림 내용 검증
    assertEquals(AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REQUEST.toString(), titleCaptor.getValue());
    assertEquals(AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REQUEST.getContent(mockWorkerUser.getUsername()), contentCaptor.getValue());

    // 3. worker와 salary가 생성되었는지 검증
    verify(workerRepository, times(1)).create(any(Worker.class));
    verify(salaryRepository, times(1)).create(any(com.moup.server.model.entity.Salary.class));
  }

  @Test
  @DisplayName("TODO 테스트: 근무지 참가 신청 - FCM 알림 발송 실패")
  void joinWorkplace_Fail_FCMSendError() throws FirebaseMessagingException {
    // given
    String inviteCode = "TESTCODE";
    Long workplaceId = 10L;
    Long ownerId = 1L;

    WorkplaceJoinRequest request = WorkplaceJoinRequest.builder()
        .inviteCode(inviteCode)
        // 2. SalaryCreateRequest를 Builder로 생성 (NoArgsConstructor 사용 X)
        .salaryCreateRequest(SalaryCreateRequest.builder().build())
        .build();

    // (필수 Mocking 설정)
    when(inviteCodeService.findWorkplaceIdByInviteCode(inviteCode.toUpperCase())).thenReturn(workplaceId);
    when(workplaceRepository.existsById(workplaceId)).thenReturn(true);
    when(workerRepository.existsByUserIdAndWorkplaceId(mockWorkerUser.getId(), workplaceId)).thenReturn(false);
    when(workplaceRepository.findOwnerId(workplaceId)).thenReturn(ownerId);

    // 5. FCMService가 호출되면 FirebaseMessagingException 예외를 던지도록 설정
    fcmService.sendToSingleUser(anyLong(), anyLong(), anyString(), anyString(), null);

    // when & then
    // 1. CustomFirebaseMessagingException 예외가 발생하는지 검증
    assertThrows(CustomFirebaseMessagingException.class, () -> {
      workplaceService.joinWorkplace(mockWorkerUser, request);
    });

    // 2. @Transactional에 의해 롤백되어야 하므로, worker와 salary는 생성(create)되면 안 됨
    verify(workerRepository, never()).create(any(Worker.class));
    verify(salaryRepository, never()).create(any(com.moup.server.model.entity.Salary.class));
  }
}
