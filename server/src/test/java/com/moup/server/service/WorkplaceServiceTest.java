package com.moup.server.service;

import com.moup.global.infra.fcm.FCMService;
import com.moup.domain.workplace.application.InviteCodeService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.workplace.application.WorkplaceService;
import com.moup.domain.alarm.domain.AlarmContent;
import com.moup.domain.alarm.domain.AlarmTitle;
import com.moup.global.common.type.Role;
import com.moup.domain.workplace.exception.WorkplaceLimitExceededException;
import com.moup.domain.user.dto.OwnerWorkplaceCreateRequest;
import com.moup.domain.salary.dto.SalaryCreateRequest;
import com.moup.domain.workplace.dto.WorkplaceJoinRequest;
import com.moup.domain.user.domain.User;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
  void joinWorkplace_Success_FCMSend() {
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
    // 초대코드 조회는 레이트 리밋을 거친다 (무차별 대입 방어)
    when(inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(mockWorkerUser.getId(), inviteCode.toUpperCase()))
        .thenReturn(workplaceId);
    // 2. 근무지 존재 확인
    when(workplaceRepository.existsById(workplaceId)).thenReturn(true);
    // 3. 이미 가입된 유저가 아님
    when(workerRepository.existsByUserIdAndWorkplaceId(mockWorkerUser.getId(), workplaceId)).thenReturn(false);
    // 4. 근무지 사장님 ID 찾기
    when(workplaceRepository.findOwnerId(workplaceId)).thenReturn(ownerId);

    // 5. FCMService가 호출되면 아무것도 하지 않음 (성공 시뮬레이션)
    // void mock은 기본이 no-op이라 스텁이 필요 없다.
    // 예전 코드는 매처(anyLong 등)와 생 null을 섞어 InvalidUseOfMatchersException을 냈다.

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
        contentCaptor.capture(),   // content
        // 매처를 쓰는 호출에는 생 null을 섞을 수 없다(InvalidUseOfMatchersException).
        // joinWorkplace는 실제로 WorkplaceJoinPayload를 넘기므로 null 자체가 틀린 기대값이었다.
        any()
    );

    // 2. 전송된 알림 내용 검증
    assertEquals(AlarmTitle.ALARM_TITLE_WORKPLACE_JOIN_REQUEST.getTitle(), titleCaptor.getValue());
    assertEquals(AlarmContent.ALARM_CONTENT_WORKPLACE_JOIN_REQUEST.getContent(mockWorkerUser.getUsername()), contentCaptor.getValue());

    // 3. worker와 salary가 생성되었는지 검증
    verify(workerRepository, times(1)).create(any(Worker.class));
    verify(salaryRepository, times(1)).create(any(Salary.class));
  }

  @Test
  @DisplayName("참가 처리는 푸시보다 먼저 확정된다 — 푸시 실패가 참가를 되돌리지 않는다")
  void joinWorkplace_ConfirmsBeforePush() {
    // given
    String inviteCode = "TESTCODE";
    Long workplaceId = 10L;
    Long ownerId = 1L;

    WorkplaceJoinRequest request = WorkplaceJoinRequest.builder()
        .inviteCode(inviteCode)
        .salaryCreateRequest(SalaryCreateRequest.builder().build())
        .build();

    // 초대코드 조회는 레이트 리밋을 거친다 (무차별 대입 방어)
    when(inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(mockWorkerUser.getId(), inviteCode.toUpperCase()))
        .thenReturn(workplaceId);
    when(workplaceRepository.existsById(workplaceId)).thenReturn(true);
    when(workerRepository.existsByUserIdAndWorkplaceId(mockWorkerUser.getId(), workplaceId)).thenReturn(false);
    when(workplaceRepository.findOwnerId(workplaceId)).thenReturn(ownerId);

    // when
    workplaceService.joinWorkplace(mockWorkerUser, request);

    // then — 예전에는 푸시 실패가 트랜잭션을 롤백시켜 **참가 자체가 무산**됐다.
    // 이제 worker·salary를 먼저 만들고 푸시는 커밋 이후 best-effort로 나간다.
    InOrder inOrder = inOrder(workerRepository, salaryRepository, fcmService);
    inOrder.verify(workerRepository).create(any(Worker.class));
    inOrder.verify(salaryRepository).create(any(Salary.class));
    inOrder.verify(fcmService).sendToSingleUser(anyLong(), eq(ownerId), anyString(), anyString(), any());
  }
}
