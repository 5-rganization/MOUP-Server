package com.moup.server.service;

import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.application.SalaryChangeNotifier;
import com.moup.domain.salary.application.SalaryUpdateService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.dto.SalaryCreateRequest;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.application.WorkerService;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.WorkerSummaryResponse;
import com.moup.domain.user.exception.WorkerAlreadyExistsException;
import com.moup.domain.user.exception.WorkerStateConflictException;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.application.InviteCodeService;
import com.moup.domain.workplace.application.WorkplaceService;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.domain.WorkplaceStatus;
import com.moup.domain.workplace.dto.WorkplaceJoinRequest;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.infra.fcm.FCMService;
import com.moup.global.util.PermissionVerifyUtil;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 사장님이 근무를 끝내도 기록이 남는다 (스코프 4 I4, Phase 11 D11-5).
///
/// 예전에는 거절이든 근무자 삭제든 `workers` 행을 지웠고, CASCADE로 그 알바생의 근무·급여 이력이
/// 전부 사라졌다. 재직 중인 알바생에게 거절을 한 번 보내는 것만으로 그렇게 됐다.
@ExtendWith(MockitoExtension.class)
class WorkerEmploymentEndTest {

    private static final Long WORKPLACE_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long WORKER_USER_ID = 2L;
    private static final Long WORKER_ID = 100L;

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkRepository workRepository;
    @Mock private UserRepository userRepository;
    @Mock private PermissionVerifyUtil permissionVerifyUtil;
    @Mock private SalaryCalculationService salaryCalculationService;
    @Mock private SalaryUpdateService salaryUpdateService;
    @Mock private SalaryChangeNotifier salaryChangeNotifier;
    @Mock private FCMService fcmService;
    @Mock private InviteCodeService inviteCodeService;

    @InjectMocks private WorkerService workerService;
    @InjectMocks private WorkplaceService workplaceService;

    private final Workplace workplace = Workplace.builder().id(WORKPLACE_ID).ownerId(OWNER_ID).workplaceName("근무지").build();

    private static Worker.WorkerBuilder worker() {
        return Worker.builder().id(WORKER_ID).userId(WORKER_USER_ID).workplaceId(WORKPLACE_ID)
                .isAccepted(true).isNowWorking(false);
    }

    private void given(Worker worker) {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        when(workerRepository.findByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID)).thenReturn(Optional.of(worker));
    }

    private void verifyNoRowDeleted() {
        verify(workerRepository, never()).deleteByIdAndWorkplaceId(anyLong(), anyLong());
        verify(workerRepository, never()).delete(any(), any(), any());
    }

    // ================= 거절 =================

    @Test
    @DisplayName("이미 승인된 근무자는 거절할 수 없다 — 기록이 지워지지 않는다")
    void 승인된_근무자_거절_불가() {
        given(worker().build());

        assertThrows(WorkerStateConflictException.class,
                () -> workerService.rejectWorker(OWNER_ID, WORKPLACE_ID, WORKER_ID));
        verifyNoRowDeleted();
        verify(workerRepository, never()).updateLeftAt(anyLong(), anyLong());
        verifyNoInteractions(fcmService);
    }

    @Test
    @DisplayName("기록이 없는 승인 대기자를 거절하면 행을 지우고 알린다")
    void 대기자_거절() {
        given(worker().isAccepted(false).build());
        when(workRepository.existsByWorkerId(WORKER_ID)).thenReturn(false);

        workerService.rejectWorker(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verify(workerRepository).deleteByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID);
        verify(fcmService).sendToSingleUser(eq(OWNER_ID), eq(WORKER_USER_ID), anyString(), anyString(), isNull());
    }

    /// 근무가 끝났다가 다시 참여 요청을 보낸 사람이다. 재참여를 거절했다고 지난 기록까지 지우면 안 된다.
    @Test
    @DisplayName("기록이 있는 승인 대기자(재참여자)를 거절하면 행을 지우지 않고 근무 종료로 되돌린다")
    void 재참여자_거절() {
        given(worker().isAccepted(false).build());
        when(workRepository.existsByWorkerId(WORKER_ID)).thenReturn(true);

        workerService.rejectWorker(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verifyNoRowDeleted();
        verify(workerRepository).updateLeftAt(WORKER_ID, WORKPLACE_ID);
    }

    // ================= 승인 =================

    @Test
    @DisplayName("이미 승인된 근무자를 다시 승인하면 아무것도 하지 않는다 — 푸시가 다시 나가지 않는다")
    void 재승인_멱등() {
        given(worker().build());

        workerService.acceptWorker(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verify(workerRepository, never()).updateIsAccepted(any(), any(), any(), any());
        verifyNoInteractions(fcmService);
    }

    @Test
    @DisplayName("근무가 끝난 근무자는 승인할 수 없다")
    void 종료된_근무자_승인_불가() {
        given(worker().leftAt(LocalDateTime.now()).build());

        assertThrows(WorkerStateConflictException.class,
                () -> workerService.acceptWorker(OWNER_ID, WORKPLACE_ID, WORKER_ID));
    }

    // ================= 근무 종료 =================

    @Test
    @DisplayName("재직자를 내보내면 행은 남기고 내일 이후 근무만 지운 뒤 다시 계산한다")
    void 근무_종료() {
        given(worker().build());
        when(workRepository.existsByWorkerId(WORKER_ID)).thenReturn(true);
        Salary salary = Salary.builder().id(1L).workerId(WORKER_ID).build();
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(salary));
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);

        workerService.deleteWorkerForOwner(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verifyNoRowDeleted();
        verify(workRepository).deleteAllByWorkerIdAfterDate(WORKER_ID, today);
        verify(workerRepository).updateLeftAt(WORKER_ID, WORKPLACE_ID);
        verify(salaryCalculationService).recalculateWorkWeek(WORKER_ID, today, salary);
        verify(salaryCalculationService).recalculateEstimatedNetIncomeForMonth(
                WORKER_ID, today.getYear(), today.getMonthValue(), salary);
    }

    @Test
    @DisplayName("근무 중인 근무자는 내보낼 수 없다 — 열린 근무가 아무도 닫을 수 없게 남는다")
    void 근무_중_종료_불가() {
        given(worker().isNowWorking(true).build());
        when(workRepository.existsByWorkerId(WORKER_ID)).thenReturn(true);

        assertThrows(WorkerStateConflictException.class,
                () -> workerService.deleteWorkerForOwner(OWNER_ID, WORKPLACE_ID, WORKER_ID));
        verify(workerRepository, never()).updateLeftAt(anyLong(), anyLong());
        verify(workRepository, never()).deleteAllByWorkerIdAfterDate(anyLong(), any());
    }

    @Test
    @DisplayName("이미 끝난 근무자를 다시 내보내도 아무것도 하지 않는다")
    void 종료_멱등() {
        given(worker().leftAt(LocalDateTime.now()).build());

        workerService.deleteWorkerForOwner(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verifyNoRowDeleted();
        verify(workerRepository, never()).updateLeftAt(anyLong(), anyLong());
        verifyNoInteractions(workRepository);
    }

    @Test
    @DisplayName("user_id가 NULL인 과거 행도 NPE 없이 처리한다 (스코프 4 탈퇴 신호 #4)")
    void 레거시_NULL_행() {
        given(worker().userId(null).isAccepted(false).build());
        when(workRepository.existsByWorkerId(WORKER_ID)).thenReturn(false);

        workerService.deleteWorkerForOwner(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        verify(workerRepository).deleteByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID);
    }

    // ================= 쓰기 차단 =================

    @Test
    @DisplayName("근무가 끝난 근무자의 쓰기는 막힌다")
    void 종료된_근무자_쓰기_차단() {
        PermissionVerifyUtil real = new PermissionVerifyUtil(userRepository);
        when(userRepository.isWithdrawn(OWNER_ID)).thenReturn(false);

        assertThrows(InvalidPermissionAccessException.class,
                () -> real.verifyWritable(worker().leftAt(LocalDateTime.now()).build(), OWNER_ID));
        assertDoesNotThrow(() -> real.verifyWritable(worker().build(), OWNER_ID));
    }

    // ================= 목록 =================

    @Test
    @DisplayName("활성 근무자 목록에서 근무가 끝난 사람과 탈퇴자를 뺀다 (스코프 4 탈퇴 신호 #3)")
    void 활성_목록() {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        Worker active = worker().build();
        Worker left = worker().id(101L).userId(3L).leftAt(LocalDateTime.now()).build();
        Worker withdrawn = worker().id(102L).userId(4L).build();
        when(workerRepository.findAllByWorkplaceIdAndUserIdNot(WORKPLACE_ID, OWNER_ID)).thenReturn(List.of(active, left, withdrawn));
        when(userRepository.findAllByIdListIn(any())).thenReturn(List.of(
                User.builder().id(WORKER_USER_ID).nickname("재직").build(),
                User.builder().id(3L).nickname("종료").build(),
                User.builder().id(4L).nickname("탈퇴한 사용자").isDeleted(true).build()));

        List<WorkerSummaryResponse> list = workerService.getActiveWorkerList(OWNER_ID, WORKPLACE_ID).getWorkerSummaryInfoList();

        assertEquals(List.of(WORKER_ID), list.stream().map(WorkerSummaryResponse::getWorkerId).toList());
    }

    // ================= 재참여 =================

    private WorkplaceJoinRequest joinRequest() {
        return WorkplaceJoinRequest.builder().inviteCode("MUP234").workerBasedLabelColor("RED")
                .salaryCreateRequest(SalaryCreateRequest.builder().hourlyRate(11_000).build()).build();
    }

    private User workerUser() {
        return User.builder().id(WORKER_USER_ID).nickname("알바생").build();
    }

    /// 기록 보존을 위해 행이 남아 있어 새로 만들 수 없다(UNIQUE). 같은 행을 다시 연다.
    @Test
    @DisplayName("근무가 끝난 근무지에 다시 참여하면 같은 행을 승인 대기로 되돌린다")
    void 재참여() {
        when(inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(WORKER_USER_ID, "MUP234")).thenReturn(WORKPLACE_ID);
        when(workplaceRepository.existsById(WORKPLACE_ID)).thenReturn(true);
        when(workerRepository.findByUserIdAndWorkplaceId(WORKER_USER_ID, WORKPLACE_ID))
                .thenReturn(Optional.of(worker().leftAt(LocalDateTime.now()).build()));
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(Salary.builder().id(7L).workerId(WORKER_ID).build()));

        workplaceService.joinWorkplace(workerUser(), joinRequest());

        verify(workerRepository).reactivate(WORKER_ID, "RED");
        verify(workerRepository, never()).create(any());
        ArgumentCaptor<Salary> saved = ArgumentCaptor.forClass(Salary.class);
        verify(salaryRepository).update(saved.capture());
        assertEquals(7L, saved.getValue().getId());
        assertEquals(11_000, saved.getValue().getHourlyRate());
    }

    @Test
    @DisplayName("재직 중인 근무지에 다시 참여하면 409")
    void 재직_중_재참여_불가() {
        when(inviteCodeService.findWorkplaceIdByInviteCodeWithRateLimit(WORKER_USER_ID, "MUP234")).thenReturn(WORKPLACE_ID);
        when(workplaceRepository.existsById(WORKPLACE_ID)).thenReturn(true);
        when(workerRepository.findByUserIdAndWorkplaceId(WORKER_USER_ID, WORKPLACE_ID)).thenReturn(Optional.of(worker().build()));

        assertThrows(WorkerAlreadyExistsException.class, () -> workplaceService.joinWorkplace(workerUser(), joinRequest()));
        verify(workerRepository, never()).reactivate(anyLong(), any());
    }

    @Test
    @DisplayName("알바생 근무지 목록에서 근무가 끝난 곳은 WORKER_LEFT다")
    void 근무지_상태() {
        when(workerRepository.findAllByUserId(WORKER_USER_ID)).thenReturn(List.of(worker().leftAt(LocalDateTime.now()).build()));
        when(workplaceRepository.findAllByIdListIn(List.of(WORKPLACE_ID))).thenReturn(List.of(workplace));
        when(userRepository.findWithdrawnIdsIn(List.of(OWNER_ID))).thenReturn(List.of());

        assertEquals(WorkplaceStatus.WORKER_LEFT,
                workplaceService.getAllWorkplace(WORKER_USER_ID, false).get(0).getStatus());
    }

    // ================= SQL =================

    private static String render(Class<?> mapper, String id) {
        Configuration configuration = new Configuration();
        configuration.addMapper(mapper);
        return configuration.getMappedStatement(mapper.getName() + "." + id)
                .getBoundSql(null).getSql().replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("근무 종료·삭제 SQL이 의도한 행만 건드린다")
    void SQL() {
        String endSql = render(WorkerRepository.class, "updateLeftAt");
        String deleteSql = render(WorkerRepository.class, "deleteByIdAndWorkplaceId");
        String futureSql = render(WorkRepository.class, "deleteAllByWorkerIdAfterDate");

        assertAll(
                () -> assertTrue(endSql.contains("left_at IS NULL"), "이미 끝난 관계의 종료 시각을 덮어쓴다: " + endSql),
                () -> assertFalse(deleteSql.contains("user_id"), "user_id가 NULL인 행이 안 지워진다: " + deleteSql),
                () -> assertTrue(futureSql.contains("work_date > ?"), "오늘 근무까지 지운다: " + futureSql)
        );
    }
}
