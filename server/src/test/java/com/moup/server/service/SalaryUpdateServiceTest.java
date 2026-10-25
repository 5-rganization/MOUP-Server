package com.moup.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moup.domain.salary.application.SalaryCalculationService;
import com.moup.domain.salary.application.SalaryUpdateService;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.salary.domain.SalaryApplyFrom;
import com.moup.domain.salary.domain.SalaryCalculation;
import com.moup.domain.salary.domain.SalaryType;
import com.moup.domain.salary.dto.SalaryDetailResponse;
import com.moup.domain.salary.dto.SalaryUpdateRequest;
import com.moup.domain.salary.mapper.SalaryRepository;
import com.moup.domain.user.dto.OwnerWorkerUpdateRequest;
import com.moup.domain.work.mapper.WorkRepository;
import com.moup.global.error.InvalidFieldFormatException;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// 급여 설정 부분 수정 (스코프 3 I-8 · I-9, Phase 11 D11-1 · D11-4).
@ExtendWith(MockitoExtension.class)
class SalaryUpdateServiceTest {

    private static final Long WORKER_ID = 100L;

    @Mock private SalaryRepository salaryRepository;
    @Mock private WorkRepository workRepository;
    @Mock private SalaryCalculationService salaryCalculationService;

    @InjectMocks private SalaryUpdateService salaryUpdateService;

    /// 주급제 고정급, 금요일 지급.
    private static Salary weeklyFixed() {
        return Salary.builder().id(1L).workerId(WORKER_ID)
                .salaryType(SalaryType.SALARY_WEEKLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_FIXED)
                .fixedRate(500_000).salaryDay(DayOfWeek.FRIDAY)
                .hasNationalPension(false).hasHealthInsurance(false).hasEmploymentInsurance(false)
                .hasIndustrialAccident(false).hasIncomeTax(false)
                .hasHolidayAllowance(false).hasNightAllowance(false)
                .build();
    }

    private static Salary hourly(int rate) {
        return weeklyFixed().toBuilder()
                .salaryType(SalaryType.SALARY_MONTHLY)
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY)
                .hourlyRate(rate).fixedRate(null).salaryDay(null).salaryDate(25)
                .hasHolidayAllowance(true)
                .build();
    }

    private static LocalDate today() {
        return LocalDate.now(SEOUL_ZONE_ID);
    }

    // ================= 부분 수정 =================

    /// 앱은 급여 요일 입력 수단이 없어 `salaryDay`를 보내지 않는다. 예전에는 전 컬럼을 덮어써
    /// 근무지를 한 번 수정하면 `salary_day`가 NULL이 되고 주급제 고정급의 월 급여가 0원이 됐다.
    @Test
    @DisplayName("보내지 않은 필드는 기존 값을 유지한다 — salaryDay가 지워지지 않는다")
    void 생략한_필드_유지() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(weeklyFixed()));

        salaryUpdateService.update(WORKER_ID, SalaryUpdateRequest.builder().hasIncomeTax(true).build());

        ArgumentCaptor<Salary> saved = ArgumentCaptor.forClass(Salary.class);
        verify(salaryRepository).update(saved.capture());
        assertAll(
                () -> assertEquals(DayOfWeek.FRIDAY, saved.getValue().getSalaryDay()),
                () -> assertEquals(500_000, saved.getValue().getFixedRate()),
                () -> assertEquals(SalaryType.SALARY_WEEKLY, saved.getValue().getSalaryType()),
                () -> assertTrue(saved.getValue().getHasIncomeTax())
        );
    }

    @Test
    @DisplayName("합친 결과가 시급제인데 시급이 없으면 422로 거절하고 아무것도 바꾸지 않는다")
    void 합친_결과로_조합_검증() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(weeklyFixed()));

        SalaryUpdateRequest toHourlyWithoutRate = SalaryUpdateRequest.builder()
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_HOURLY).build();

        assertThrows(InvalidFieldFormatException.class,
                () -> salaryUpdateService.update(WORKER_ID, toHourlyWithoutRate));
        verify(salaryRepository, never()).update(any());
        verifyNoInteractions(workRepository, salaryCalculationService);
    }

    @Test
    @DisplayName("같은 값을 다시 보내면 아무것도 하지 않고 false — 변경 알림이 나가지 않는다")
    void 변경_없음() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));

        boolean changed = salaryUpdateService.update(WORKER_ID,
                SalaryUpdateRequest.builder().hourlyRate(10_030).hasHolidayAllowance(true).build());

        assertFalse(changed);
        verify(salaryRepository, never()).update(any());
        verifyNoInteractions(workRepository, salaryCalculationService);
    }

    // ================= 적용 시점 =================

    /// 예전에는 근무 행의 시급 스냅샷을 아예 바꾸지 않아 **시급을 올려도 이미 잡아 둔 근무는 옛 시급**이었다.
    /// 반대로 공제 재계산은 이번 달 1일부터 해 지난 근무일까지 바뀌었다(스코프 4 Q1: 의도 아님).
    @Test
    @DisplayName("기본은 오늘 근무부터 새 시급·수당 스냅샷을 적용한다")
    void 기본은_오늘부터() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));

        assertTrue(salaryUpdateService.update(WORKER_ID,
                SalaryUpdateRequest.builder().hourlyRate(11_000).hasNightAllowance(true).build()));

        verify(workRepository).updateSalarySnapshotFrom(WORKER_ID, today(), 11_000, true, true, false);
        verify(workRepository).findWorkDatesFrom(WORKER_ID, today());
    }

    @Test
    @DisplayName("THIS_MONTH면 이번 달 1일 근무부터 적용한다")
    void 이번_달부터() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));

        salaryUpdateService.update(WORKER_ID, SalaryUpdateRequest.builder()
                .hourlyRate(11_000).applyFrom(SalaryApplyFrom.THIS_MONTH).build());

        verify(workRepository).updateSalarySnapshotFrom(
                eq(WORKER_ID), eq(today().withDayOfMonth(1)), eq(11_000), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("고정급으로 바꾸면 근무 행 시급은 0이 된다 (근무 등록과 같은 규칙)")
    void 고정급_스냅샷() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));

        salaryUpdateService.update(WORKER_ID, SalaryUpdateRequest.builder()
                .salaryCalculation(SalaryCalculation.SALARY_CALCULATION_FIXED).fixedRate(2_000_000).build());

        verify(workRepository).updateSalarySnapshotFrom(eq(WORKER_ID), any(), eq(0), anyBoolean(), anyBoolean(), anyBoolean());
    }

    /// 주마다 한 번만, 그리고 **그 주의 첫 근무일**로 재계산한다. 월요일을 넘기면 주가 월 경계에 걸칠 때
    /// `recalculateWorkWeek`이 지난달 공제까지 새 설정으로 다시 계산한다.
    @Test
    @DisplayName("근무가 있는 주마다 한 번, 그 주의 첫 근무일로 재계산한다")
    void 주별_재계산() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));
        LocalDate thu = LocalDate.of(2026, 10, 1);      // 목요일 — 그 주 월요일은 9월 28일
        LocalDate fri = thu.plusDays(1);
        LocalDate nextTue = thu.plusDays(5);
        when(workRepository.findWorkDatesFrom(eq(WORKER_ID), any())).thenReturn(List.of(thu, fri, nextTue));
        when(workRepository.findDistinctWorkMonthsAfter(eq(WORKER_ID), any())).thenReturn(List.of());

        salaryUpdateService.update(WORKER_ID, SalaryUpdateRequest.builder().hourlyRate(11_000).build());

        ArgumentCaptor<LocalDate> dates = ArgumentCaptor.forClass(LocalDate.class);
        verify(salaryCalculationService, times(2)).recalculateWorkWeek(eq(WORKER_ID), dates.capture(), any());
        assertEquals(List.of(thu, nextTue), dates.getAllValues());
    }

    /// 4대보험·세금은 월 단위라 오늘부터 적용해도 이번 달 전체 공제가 새 설정을 따른다.
    @Test
    @DisplayName("공제는 적용 시작일이 든 달부터 월 단위로 다시 계산한다")
    void 월별_공제_재계산() {
        when(salaryRepository.findByWorkerId(WORKER_ID)).thenReturn(Optional.of(hourly(10_030)));
        when(workRepository.findDistinctWorkMonthsAfter(WORKER_ID, today().withDayOfMonth(1)))
                .thenReturn(List.of(new WorkRepository.WorkMonthDto(2026, 10), new WorkRepository.WorkMonthDto(2026, 11)));

        salaryUpdateService.update(WORKER_ID, SalaryUpdateRequest.builder().hasIncomeTax(true).build());

        verify(salaryCalculationService).recalculateEstimatedNetIncomeForMonth(eq(WORKER_ID), eq(2026), eq(10), any());
        verify(salaryCalculationService).recalculateEstimatedNetIncomeForMonth(eq(WORKER_ID), eq(2026), eq(11), any());
    }

    // ================= 응답·요청 계약 =================

    /// 수정 폼은 이 응답을 채워 되보낸다. 예전 조립 코드는 두 필드를 빠뜨려 고정급 금액이 0으로 채워졌다.
    @Test
    @DisplayName("급여 상세 응답은 fixedRate와 salaryDay를 포함한다")
    void 상세_응답_필드() {
        SalaryDetailResponse response = SalaryDetailResponse.from(weeklyFixed());

        assertEquals(500_000, response.getFixedRate());
        assertEquals(DayOfWeek.FRIDAY, response.getSalaryDay());
    }

    /// 사장님은 시급만 바꾸려고 급여 필드 하나만 보낼 수 있어야 한다. 라벨색도 생략할 수 있다.
    /// 예전 공통 부모의 `@JsonTypeInfo(DEDUCTION)`은 판별 키(라벨색)가 없으면 이 요청을 거부할 수 있었다.
    @Test
    @DisplayName("사장님 근무자 수정은 급여 필드 하나만 보내도 역직렬화된다")
    void 부분_요청_역직렬화() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

        OwnerWorkerUpdateRequest request = mapper.readValue(
                "{\"salaryUpdateRequest\":{\"hourlyRate\":11000}}", OwnerWorkerUpdateRequest.class);

        assertNull(request.getOwnerBasedLabelColor());
        assertEquals(11_000, request.getSalaryUpdateRequest().getHourlyRate());
        assertNull(request.getSalaryUpdateRequest().getSalaryDay());
        assertEquals(SalaryApplyFrom.TODAY, request.getSalaryUpdateRequest().resolveApplyFrom());
    }

    /// 스냅샷 갱신이 적용 시작일 이후만 건드리는지 **렌더링된 SQL로** 고정한다. Mockito로는 WHERE가 빠져도 통과한다.
    @Test
    @DisplayName("스냅샷 갱신 SQL은 그 근무자의 적용 시작일 이후 근무만 바꾼다")
    void 스냅샷_SQL() {
        Configuration configuration = new Configuration();
        configuration.addMapper(WorkRepository.class);
        String sql = configuration.getMappedStatement(WorkRepository.class.getName() + ".updateSalarySnapshotFrom")
                .getBoundSql(null).getSql().replaceAll("\\s+", " ");

        assertAll(
                () -> assertTrue(sql.contains("worker_id = ?"), sql),
                () -> assertTrue(sql.contains("work_date >= ?"), sql),
                () -> assertFalse(sql.contains("base_pay"), "급여 금액은 재계산이 채운다: " + sql)
        );
    }
}
