package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.dto.WorkSummaryResponse;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.InvalidPermissionAccessException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 매장 캘린더(`getAllWorkByWorkplace`)의 가시 범위 (스코프 2 I1 · I2, Phase 11 D11-5).
@ExtendWith(MockitoExtension.class)
class StoreCalendarVisibilityTest {

    private static final Long WORKPLACE_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoutineService routineService;
    @Mock private SalaryCalculationService salaryCalculationService;
    @Mock private PermissionVerifyUtil permissionVerifyUtil;
    @Mock private RoutineTaskCompletionRepository routineTaskCompletionRepository;

    @InjectMocks private WorkService workService;

    private final Workplace workplace = Workplace.builder().id(WORKPLACE_ID).ownerId(OWNER_ID).workplaceName("근무지").build();
    private final Worker me = Worker.builder().id(100L).userId(2L).workplaceId(WORKPLACE_ID).isAccepted(true).build();
    private final Worker colleague = Worker.builder().id(101L).userId(3L).workplaceId(WORKPLACE_ID).isAccepted(true).build();

    private static Work work(Long id, Long workerId) {
        LocalDateTime start = LocalDate.of(2026, 10, 5).atTime(9, 0);
        return Work.builder().id(id).workerId(workerId).workDate(start.toLocalDate())
                .startTime(start).endTime(start.plusHours(4)).restTimeMinutes(0)
                .grossIncome(40_000).estimatedNetIncome(38_000).build();
    }

    private void givenStore(Worker requester, Long requesterUserId) {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        when(workerRepository.findByUserIdAndWorkplaceId(requesterUserId, WORKPLACE_ID)).thenReturn(Optional.ofNullable(requester));
    }

    private void givenWorks() {
        when(workerRepository.findAllByWorkplaceId(WORKPLACE_ID)).thenReturn(List.of(me, colleague));
        when(userRepository.findAllByIdListIn(any())).thenReturn(List.of(
                User.builder().id(2L).nickname("나").build(), User.builder().id(3L).nickname("동료").build()));
        when(workRepository.findAllByWorkerIdListInAndDateRange(any(), any(), any()))
                .thenAnswer(invocation -> ((List<Long>) invocation.getArgument(0)).stream()
                        .map(workerId -> work(workerId * 10, workerId)).toList());
    }

    private Map<Long, WorkSummaryResponse> byWorkId(User requester) {
        return workService.getAllWorkByWorkplace(requester, WORKPLACE_ID, MONTH).getWorkSummaryInfoList().stream()
                .collect(Collectors.toMap(WorkSummaryResponse::getWorkId, Function.identity()));
    }

    @Test
    @DisplayName("알바생에게는 동료 근무의 추정 실수령액을 가린다 — 자기 것은 보인다")
    void 동료_금액_가림() {
        givenStore(me, 2L);
        givenWorks();

        Map<Long, WorkSummaryResponse> works = byWorkId(User.builder().id(2L).build());

        assertEquals(38_000, works.get(1000L).getEstimatedNetIncome(), "내 근무");
        assertNull(works.get(1010L).getEstimatedNetIncome(), "동료 근무의 실수령액이 보인다");
    }

    @Test
    @DisplayName("사장님에게는 모든 근무의 금액이 보인다")
    void 사장님은_모두_보임() {
        givenStore(null, OWNER_ID);
        givenWorks();

        Map<Long, WorkSummaryResponse> works = byWorkId(User.builder().id(OWNER_ID).build());

        assertEquals(38_000, works.get(1010L).getEstimatedNetIncome());
    }

    /// 확정 정책 4 — 승인 대기자는 근무지 이름·사장님 정보·대기 표시만 본다.
    @Test
    @DisplayName("승인 대기자는 매장 근무표를 볼 수 없다")
    void 대기자_차단() {
        Worker pending = me.toBuilder().isAccepted(false).build();
        givenStore(pending, 2L);
        doThrow(new InvalidPermissionAccessException()).when(permissionVerifyUtil)
                .verifyWorkerPermission(2L, pending, OWNER_ID);

        assertThrows(InvalidPermissionAccessException.class,
                () -> workService.getAllWorkByWorkplace(User.builder().id(2L).build(), WORKPLACE_ID, MONTH));
        verify(workRepository, never()).findAllByWorkerIdListInAndDateRange(any(), any(), any());
    }

    @Test
    @DisplayName("근무가 끝난 알바생은 그 근무지에서 자기 기록만 보고, 수정할 수 없다")
    void 종료자는_자기_기록만() {
        Worker left = me.toBuilder().leftAt(LocalDateTime.now()).build();
        givenStore(left, 2L);
        when(workerRepository.findAllByWorkplaceId(WORKPLACE_ID)).thenReturn(List.of(left, colleague));
        when(userRepository.findAllByIdListIn(any())).thenReturn(List.of(User.builder().id(2L).nickname("나").build()));
        when(workRepository.findAllByWorkerIdListInAndDateRange(eq(List.of(100L)), any(), any()))
                .thenReturn(List.of(work(1000L, 100L)));

        Map<Long, WorkSummaryResponse> works = byWorkId(User.builder().id(2L).build());

        assertEquals(List.of(1000L), List.copyOf(works.keySet()));
        assertFalse(works.get(1000L).getIsEditable());
    }
}
