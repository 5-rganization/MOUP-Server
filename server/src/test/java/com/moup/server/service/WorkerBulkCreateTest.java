package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.FailedWorkerInfo;
import com.moup.domain.user.dto.WorkersWorkCreateRequest;
import com.moup.domain.user.dto.WorkersWorkCreateResponse;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.InvalidFieldFormatException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 사장님의 여러 근무자 근무 일괄 생성 (스코프 2 I5 · 4 M4).
@ExtendWith(MockitoExtension.class)
class WorkerBulkCreateTest {

    private static final Long OWNER_ID = 1L;
    private static final Long WORKPLACE_ID = 10L;
    private static final LocalDateTime START = LocalDate.of(2026, 10, 13).atTime(9, 0);

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

    private final Worker active = Worker.builder().id(100L).userId(2L).workplaceId(WORKPLACE_ID).isAccepted(true).build();

    private WorkersWorkCreateRequest request(List<Long> workerIds) {
        return WorkersWorkCreateRequest.builder().workerIdList(workerIds)
                .startTime(START.atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(START.plusHours(4).atZone(SEOUL_ZONE_ID).toInstant())
                .restTimeMinutes(0).repeatDays(List.of()).build();
    }

    private void givenStore() {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(java.util.Optional.of(
                Workplace.builder().id(WORKPLACE_ID).ownerId(OWNER_ID).build()));
        when(workerRepository.findAllByIdInAndWorkplaceId(any(), eq(WORKPLACE_ID))).thenReturn(List.of(active));
        when(userRepository.findAllByIdListIn(any())).thenReturn(List.of(User.builder().id(2L).nickname("알바").build()));
        lenient().when(salaryCalculationService.calculateDailyIncome(any(), anyInt(), anyBoolean()))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("배정할 수 없는 근무자는 실패 목록으로 돌려주고 나머지는 만든다 — 사유는 내부 예외 메시지가 아니다")
    void 검증_실패는_목록으로() {
        givenStore();

        WorkersWorkCreateResponse response = workService.createWorkForWorkerIdList(OWNER_ID, WORKPLACE_ID, request(List.of(100L, 999L)));

        verify(workRepository, times(1)).create(any());
        List<FailedWorkerInfo> failed = response.getFailedWorkerInfoList();
        assertEquals(1, failed.size());
        assertEquals(999L, failed.get(0).getWorkerId());
        assertEquals("해당 근무지 소속이 아닌 근무자입니다.", failed.get(0).getReason());
    }

    /// 예전에는 이것도 삼켜 "실패"로 보고했다. 그 근무자의 앞선 INSERT는 커밋되고, @Transactional 빈 안에서
    /// 난 예외였다면 커밋 시점에 요청 전체가 500이 됐다.
    @Test
    @DisplayName("근무를 만들다 난 오류(시간 겹침 등)는 삼키지 않고 요청 전체를 실패시킨다")
    void 생성_중_오류는_전파() {
        givenStore();
        when(workRepository.findAllByWorkerIdAndDateRange(eq(100L), any(), any())).thenReturn(List.of(
                Work.builder().id(5L).workerId(100L).workDate(START.toLocalDate())
                        .startTime(START.plusHours(1)).endTime(START.plusHours(3)).build()));

        assertThrows(InvalidFieldFormatException.class,
                () -> workService.createWorkForWorkerIdList(OWNER_ID, WORKPLACE_ID, request(List.of(100L))));
        verify(workRepository, never()).create(any());
    }
}
