package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.util.PermissionVerifyUtil;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/// 출근 확인 전에 사용자 단위 잠금을 건다 (스코프 2 M6).
@ExtendWith(MockitoExtension.class)
class ClockInLockTest {

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

    @Test
    @DisplayName("근무 중인지 확인하기 전에 그 사용자의 근무자 행을 잠근다")
    void 확인_전에_잠근다() {
        when(workerRepository.findByUserIdAndWorkplaceId(2L, 10L)).thenReturn(Optional.of(
                Worker.builder().id(100L).userId(2L).workplaceId(10L).isAccepted(true).build()));
        when(workplaceRepository.findById(10L)).thenReturn(Optional.of(Workplace.builder().id(10L).ownerId(1L).build()));
        when(workRepository.findEligibleWorkForClockIn(any(), any())).thenReturn(Optional.empty());

        workService.updateActualStartTime(2L, 10L);

        InOrder order = inOrder(workerRepository);
        order.verify(workerRepository).lockAllByUserId(2L);
        order.verify(workerRepository).existsByUserIdAndIsNowWorking(2L, true);
    }

    @Test
    @DisplayName("잠금 SQL은 FOR UPDATE다")
    void 잠금_SQL() {
        Configuration configuration = new Configuration();
        configuration.addMapper(WorkerRepository.class);
        String sql = configuration.getMappedStatement(WorkerRepository.class.getName() + ".lockAllByUserId")
                .getBoundSql(Map.of("userId", 2L)).getSql();

        assertTrue(sql.endsWith("FOR UPDATE"), sql);
    }
}
