package com.moup.server.service;

import com.moup.domain.routine.application.RoutineService;
import com.moup.domain.routine.mapper.RoutineTaskCompletionRepository;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.domain.user.mapper.WorkerRepository;
import com.moup.domain.work.application.WorkService;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.dto.MyWorkCreateRequest;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.error.InvalidFieldFormatException;
import com.moup.global.util.PermissionVerifyUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// 근무 분·야간 분 계산을 1분 루프에서 구간 산술로 바꿨다 (스코프 2 I7 · 3 I-10).
/// 결과가 예전 루프와 **한 분도 다르지 않아야** 한다 — 여기의 `reference`가 예전 코드 그대로다.
@ExtendWith(MockitoExtension.class)
class WorkTimeBoundsTest {

    @InjectMocks private SalaryCalculationService salaryCalculationService;

    @Mock private WorkRepository workRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoutineService routineService;
    @Mock private SalaryCalculationService mockedSalaryCalculationService;
    @Mock private PermissionVerifyUtil permissionVerifyUtil;
    @Mock private RoutineTaskCompletionRepository routineTaskCompletionRepository;
    @InjectMocks private WorkService workService;

    /// 예전 `calculateDailyIncome`의 루프.
    private static long[] reference(LocalDateTime start, LocalDateTime end) {
        long gross = 0, night = 0;
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            gross++;
            LocalTime t = cursor.toLocalTime();
            if (!t.isBefore(LocalTime.of(22, 0)) || t.isBefore(LocalTime.of(6, 0))) {
                night++;
            }
            cursor = cursor.plusMinutes(1);
        }
        return new long[]{gross, night};
    }

    private Work compute(LocalDateTime start, LocalDateTime end) {
        Work work = Work.builder().workDate(start.toLocalDate()).startTime(start).endTime(end)
                .restTimeMinutes(0).hourlyRate(10_000).isAbsent(false).build();
        return salaryCalculationService.calculateDailyIncome(work, 0, true);
    }

    @Test
    @DisplayName("무작위 근무 5만 건에서 근무 분·야간 분이 예전 루프와 같다 (초 단위 시각 포함)")
    void 예전_루프와_동일() {
        Random random = new Random(20261025);
        LocalDateTime base = LocalDate.of(2026, 1, 1).atStartOfDay();
        for (int i = 0; i < 50_000; i++) {
            LocalDateTime start = base.plusSeconds(random.nextInt(366 * 24 * 3600))
                    .plusNanos(random.nextBoolean() ? 0 : random.nextInt(1_000_000_000));
            // 0 ~ 3일. 상한(24시간)은 입력 검증이 막지만 계산부는 레거시 행도 받으므로 그 너머까지 본다.
            LocalDateTime end = start.plusSeconds(random.nextInt(3 * 24 * 3600 + 1))
                    .plusNanos(random.nextBoolean() ? 0 : random.nextInt(1_000_000_000));

            long[] expected = reference(start, end);
            Work actual = compute(start, end);

            String context = start + " → " + end;
            assertEquals(expected[0], (long) actual.getGrossWorkMinutes(), "근무 분 " + context);
            assertEquals(expected[1], (long) actual.getNightWorkMinutes(), "야간 분 " + context);
        }
    }

    @Test
    @DisplayName("경계: 22:00 정각 시작, 06:00 정각 종료, 자정을 넘는 근무")
    void 경계() {
        LocalDate day = LocalDate.of(2026, 10, 5);
        assertEquals(480, compute(day.atTime(22, 0), day.plusDays(1).atTime(6, 0)).getNightWorkMinutes());
        assertEquals(0, compute(day.atTime(6, 0), day.atTime(22, 0)).getNightWorkMinutes());
        // 21:59:30에 시작한 분은 주간, 22:00:30에 시작한 분은 야간이다. 분이 야간인지는 그 분이 시작하는 시각으로 정한다.
        assertEquals(1, compute(day.atTime(21, 59, 30), day.atTime(22, 1)).getNightWorkMinutes());
        assertEquals(0, compute(day.atTime(21, 59, 30), day.atTime(22, 0, 30)).getNightWorkMinutes());
    }

    /// 예전 루프로는 이 근무 하나에 약 526만 번을 돌았다.
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    @DisplayName("10년짜리 레거시 행도 즉시 계산한다")
    void 긴_근무도_상수_시간() {
        Work result = compute(LocalDate.of(2020, 1, 1).atStartOfDay(), LocalDate.of(2030, 1, 1).atStartOfDay());

        assertEquals(3653L * 1440, (long) result.getGrossWorkMinutes());
        assertEquals(3653L * 480, (long) result.getNightWorkMinutes());
    }

    // ================= 입력 검증 =================

    @ParameterizedTest(name = "{0}분 근무, 휴게 {1}분 → 거부 ({2})")
    @DisplayName("근무 시각·휴게시간이 말이 안 되면 422로 거부하고 저장하지 않는다")
    @CsvSource({
            "0, 0, '0분 근무 — 예전에는 isBefore만 봐 통과했다'",
            "-30, 0, '퇴근이 출근보다 앞'",
            "1441, 0, '24시간 초과 — 예전에는 상한이 없었다'",
            "60, 61, '휴게가 근무보다 길다'",
    })
    void 입력_검증(int workMinutes, int restMinutes, String reason) {
        Worker worker = Worker.builder().id(100L).userId(2L).workplaceId(10L).isAccepted(true).build();
        when(workerRepository.findByUserIdAndWorkplaceId(2L, 10L)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(10L)).thenReturn(Optional.of(Workplace.builder().id(10L).ownerId(1L).build()));
        LocalDateTime start = LocalDate.of(2026, 10, 5).atTime(9, 0);
        MyWorkCreateRequest request = MyWorkCreateRequest.builder()
                .routineIdList(List.of()).repeatDays(List.of())
                .startTime(start.atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(start.plusMinutes(workMinutes).atZone(SEOUL_ZONE_ID).toInstant())
                .restTimeMinutes(restMinutes).build();

        assertThrows(InvalidFieldFormatException.class, () -> workService.createMyWork(2L, 10L, request), reason);
        verify(workRepository, never()).create(any());
    }

    // ================= 겹침 (스코프 2 I4) =================

    private static final LocalDate MON = LocalDate.of(2026, 10, 5);

    private MyWorkCreateRequest request(LocalDateTime start, LocalDateTime end, List<java.time.DayOfWeek> repeatDays,
                                        LocalDate repeatEnd) {
        return MyWorkCreateRequest.builder()
                .routineIdList(List.of()).repeatDays(repeatDays).repeatEndDate(repeatEnd)
                .startTime(start.atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(end.atZone(SEOUL_ZONE_ID).toInstant())
                .restTimeMinutes(0).build();
    }

    private void givenWorkerWithWorks(Work... existing) {
        Worker worker = Worker.builder().id(100L).userId(2L).workplaceId(10L).isAccepted(true).build();
        when(workerRepository.findByUserIdAndWorkplaceId(2L, 10L)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(10L)).thenReturn(Optional.of(Workplace.builder().id(10L).ownerId(1L).build()));
        // 실제 쿼리처럼 work_date 범위로 거른다. 범위를 무시하면 "전날 근무까지 읽는다"를 검증할 수 없다.
        when(workRepository.findAllByWorkerIdAndDateRange(org.mockito.ArgumentMatchers.eq(100L), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(1);
                    LocalDate to = invocation.getArgument(2);
                    return java.util.Arrays.stream(existing)
                            .filter(work -> !work.getWorkDate().isBefore(from) && !work.getWorkDate().isAfter(to))
                            .toList();
                });
        org.mockito.Mockito.lenient().when(mockedSalaryCalculationService.calculateDailyIncome(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Work existing(long id, LocalDateTime start, LocalDateTime end) {
        return Work.builder().id(id).workerId(100L).workDate(start.toLocalDate()).startTime(start).endTime(end).build();
    }

    @Test
    @DisplayName("이미 있는 근무와 시간이 겹치면 거부한다")
    void 겹침_거부() {
        givenWorkerWithWorks(existing(1L, MON.atTime(10, 0), MON.atTime(18, 0)));

        assertThrows(InvalidFieldFormatException.class,
                () -> workService.createMyWork(2L, 10L, request(MON.atTime(12, 0), MON.atTime(20, 0), List.of(), null)));
        verify(workRepository, never()).create(any());
    }

    @Test
    @DisplayName("끝나는 시각에 바로 시작하는 근무는 겹치지 않는다")
    void 맞닿음_허용() {
        givenWorkerWithWorks(existing(1L, MON.atTime(10, 0), MON.atTime(18, 0)));

        workService.createMyWork(2L, 10L, request(MON.atTime(18, 0), MON.atTime(22, 0), List.of(), null));

        verify(workRepository).create(any());
    }

    @Test
    @DisplayName("전날 시작해 자정을 넘긴 근무와 겹쳐도 잡는다")
    void 전날_밤샘_근무() {
        givenWorkerWithWorks(existing(1L, MON.minusDays(1).atTime(22, 0), MON.atTime(6, 0)));

        assertThrows(InvalidFieldFormatException.class,
                () -> workService.createMyWork(2L, 10L, request(MON.atTime(5, 0), MON.atTime(9, 0), List.of(), null)));
    }

    /// 반복 근무 하나라도 기존 근무와 겹치면 전체를 거부한다. 일부만 만들면 사용자는 어느 날이 빠졌는지 모른다.
    @Test
    @DisplayName("반복 근무 중 하루라도 기존 근무와 겹치면 아무것도 만들지 않는다")
    void 반복_중_하루_겹침() {
        givenWorkerWithWorks(existing(1L, MON.plusDays(2).atTime(8, 0), MON.plusDays(2).atTime(10, 0)));

        assertThrows(InvalidFieldFormatException.class, () -> workService.createMyWork(2L, 10L,
                request(MON.atTime(9, 0), MON.atTime(13, 0), List.of(java.time.DayOfWeek.values()), MON.plusDays(6))));
        verify(workRepository, never()).createBatch(any());
    }

    /// 수정하는 근무 자신과는 겹침으로 보지 않는다. 안 그러면 시간을 조금만 옮겨도 전부 거부된다.
    @Test
    @DisplayName("근무를 수정할 때 그 근무 자신과는 겹침으로 보지 않는다")
    void 수정_시_자기_제외() {
        Work current = existing(1L, MON.atTime(10, 0), MON.atTime(18, 0)).toBuilder().restTimeMinutes(0).build();
        Worker worker = Worker.builder().id(100L).userId(2L).workplaceId(10L).isAccepted(true).build();
        when(workRepository.findById(1L)).thenReturn(Optional.of(current));
        when(workerRepository.findById(100L)).thenReturn(Optional.of(worker));
        when(workplaceRepository.findById(10L)).thenReturn(Optional.of(Workplace.builder().id(10L).ownerId(1L).build()));
        when(workRepository.findAllByWorkerIdAndDateRange(org.mockito.ArgumentMatchers.eq(100L), any(), any()))
                .thenReturn(List.of(current));
        when(mockedSalaryCalculationService.calculateDailyIncome(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workService.updateMySingleWork(2L, 1L, com.moup.domain.work.dto.MyWorkUpdateRequest.builder()
                .routineIdList(List.of()).repeatDays(List.of())
                .startTime(MON.atTime(11, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .endTime(MON.atTime(19, 0).atZone(SEOUL_ZONE_ID).toInstant())
                .restTimeMinutes(0).build());

        verify(workRepository).update(any());
    }
}
