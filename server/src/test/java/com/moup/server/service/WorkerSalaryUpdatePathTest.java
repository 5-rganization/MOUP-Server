package com.moup.server.service;

import com.moup.domain.salary.application.SalaryChangeNotifier;
import com.moup.domain.salary.application.SalaryUpdateService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.dto.SalaryUpdateRequest;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.application.WorkerService;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.dto.OwnerWorkerUpdateRequest;
import com.moup.domain.user.dto.WorkerWorkerUpdateRequest;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.dto.WorkerAttendanceInfoResponse;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 근무자 수정 두 경로가 급여를 `SalaryUpdateService`로 넘기고, 바뀐 경우에만 상대에게 알리는지.
@ExtendWith(MockitoExtension.class)
class WorkerSalaryUpdatePathTest {

    private static final Long WORKPLACE_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long WORKER_USER_ID = 2L;
    private static final Long WORKER_ID = 100L;

    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkRepository workRepository;
    @Mock private PermissionVerifyUtil permissionVerifyUtil;
    @Mock private SalaryUpdateService salaryUpdateService;
    @Mock private SalaryChangeNotifier salaryChangeNotifier;

    @InjectMocks private WorkerService workerService;

    private final User workerUser = User.builder().id(WORKER_USER_ID).nickname("알바생").build();
    private final User owner = User.builder().id(OWNER_ID).nickname("사장님").build();
    private final Workplace workplace = Workplace.builder().id(WORKPLACE_ID).ownerId(OWNER_ID).workplaceName("근무지").build();
    private final Worker worker = Worker.builder().id(WORKER_ID).userId(WORKER_USER_ID).workplaceId(WORKPLACE_ID).build();
    private final SalaryUpdateRequest raise = SalaryUpdateRequest.builder().hourlyRate(11_000).build();

    private void givenMyWorker() {
        when(workerRepository.findByUserIdAndWorkplaceId(WORKER_USER_ID, WORKPLACE_ID)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
    }

    // ================= 알바생 본인 =================

    /// 같은 변경이 근무지 수정 경로에서는 막히고 이 경로에서는 통과했다(스코프 4 C4).
    @Test
    @DisplayName("사장님이 탈퇴한 근무지에서는 알바생이 급여 설정을 바꿀 수 없다")
    void 탈퇴한_근무지_차단() {
        givenMyWorker();
        doThrow(new InvalidPermissionAccessException()).when(permissionVerifyUtil).verifyWorkplaceIsWritable(OWNER_ID);

        WorkerWorkerUpdateRequest request = WorkerWorkerUpdateRequest.builder().salaryUpdateRequest(raise).build();

        assertThrows(InvalidPermissionAccessException.class,
                () -> workerService.updateMyWorker(workerUser, WORKPLACE_ID, request));
        verifyNoInteractions(salaryUpdateService, salaryChangeNotifier);
    }

    @Test
    @DisplayName("알바생이 급여를 바꾸면 사장님에게 알린다")
    void 알바생_변경_알림() {
        givenMyWorker();
        when(salaryUpdateService.update(WORKER_ID, raise)).thenReturn(true);

        workerService.updateMyWorker(workerUser, WORKPLACE_ID,
                WorkerWorkerUpdateRequest.builder().salaryUpdateRequest(raise).build());

        verify(salaryChangeNotifier).notifyOwner(workerUser, WORKPLACE_ID);
        // 라벨색을 생략했으니 건드리지 않는다
        verify(workerRepository, never()).updateWorkerBasedLabelColor(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("급여가 실제로 바뀌지 않았으면 알리지 않는다")
    void 변경_없으면_알림_없음() {
        givenMyWorker();
        when(salaryUpdateService.update(WORKER_ID, raise)).thenReturn(false);

        workerService.updateMyWorker(workerUser, WORKPLACE_ID,
                WorkerWorkerUpdateRequest.builder().workerBasedLabelColor("RED").salaryUpdateRequest(raise).build());

        verify(workerRepository).updateWorkerBasedLabelColor(WORKER_ID, WORKER_USER_ID, WORKPLACE_ID, "RED");
        verifyNoInteractions(salaryChangeNotifier);
    }

    // ================= 사장님 =================

    @Test
    @DisplayName("사장님이 알바생 급여를 바꾸면 알바생에게 알린다")
    void 사장님_변경_알림() {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        when(workerRepository.findByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID)).thenReturn(Optional.of(worker));
        when(salaryUpdateService.update(WORKER_ID, raise)).thenReturn(true);

        workerService.updateWorkerForOwner(owner, WORKPLACE_ID, WORKER_ID,
                OwnerWorkerUpdateRequest.builder().salaryUpdateRequest(raise).build());

        verify(permissionVerifyUtil).verifyOwnerPermission(OWNER_ID, OWNER_ID);
        verify(salaryChangeNotifier).notifyWorker(OWNER_ID, WORKER_USER_ID, workplace);
        verify(workerRepository, never()).updateOwnerBasedLabelColor(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("다른 근무지의 근무자 ID로는 급여를 바꿀 수 없다")
    void 남의_근무자_차단() {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        when(workerRepository.findByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workerService.updateWorkerForOwner(owner, WORKPLACE_ID, WORKER_ID,
                OwnerWorkerUpdateRequest.builder().salaryUpdateRequest(raise).build()));
        verifyNoInteractions(salaryUpdateService);
    }

    /// 사장님이 부분 수정하려면 현재 값을 알아야 한다. 예전에는 조회할 API가 없었다(스코프 3 I-8).
    @Test
    @DisplayName("사장님의 근무자 상세 조회에 급여 설정이 들어 있다")
    void 사장님_급여_조회() {
        when(workplaceRepository.findById(WORKPLACE_ID)).thenReturn(Optional.of(workplace));
        when(workerRepository.existsByIdAndWorkplaceId(WORKER_ID, WORKPLACE_ID)).thenReturn(true);
        when(workRepository.findAllByWorkerId(WORKER_ID)).thenReturn(List.of());
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(Salary.builder()
                .salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
                .hourlyRate(10_030).build()));

        WorkerAttendanceInfoResponse response = workerService.getWorkerAttendanceInfo(OWNER_ID, WORKPLACE_ID, WORKER_ID);

        assertEquals(10_030, response.getSalaryDetailInfo().getHourlyRate());
    }
}
