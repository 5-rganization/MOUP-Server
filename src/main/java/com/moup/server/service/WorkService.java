package com.moup.server.service;

import com.moup.server.exception.SalaryWorkerNotFoundException;
import com.moup.server.exception.WorkerWorkplaceNotFoundException;
import com.moup.server.model.dto.WorkCreateRequest;
import com.moup.server.model.dto.WorkCreateResponse;
import com.moup.server.model.dto.WorkDetailResponse;
import com.moup.server.model.entity.Salary;
import com.moup.server.model.entity.Work;
import com.moup.server.repository.SalaryRepository;
import com.moup.server.repository.WorkRepository;
import com.moup.server.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {
    private final WorkRepository workRepository;
    private final SalaryRepository salaryRepository;
    private final WorkerRepository workerRepository;
    private final RoutineService routineService;
    private final SalaryCalculationService salaryCalculationService;

    @Transactional
    public WorkCreateResponse createWork(Long userId, Long workplaceId, WorkCreateRequest request) {
        Long workerId = workerRepository.findByUserIdAndWorkplaceId(userId, workplaceId)
                .orElseThrow(WorkerWorkplaceNotFoundException::new).getId();
        Salary salary = salaryRepository.findByWorkerId(workerId)
                .orElseThrow(SalaryWorkerNotFoundException::new);

        // 1. Work 엔티티 생성 및 기본 정보 저장
        Work work = request.toEntity(workerId, salary.getHourlyRate());
        workRepository.create(work); // DB에 먼저 저장되어야 ID가 생성됨

        // 2. 해당 근무일이 포함된 주의 모든 근무 기록을 다시 계산 (주휴수당 때문)
        salaryCalculationService.recalculateWorkWeek(work.getWorkerId(), work.getWorkDate());

        work = request.toEntity(workerId, salary.getHourlyRate());
        workRepository.create(work);

        for (Long routineId : request.getRoutineIdList()) {
            routineService.mapRoutineToWork(userId, routineId, work.getId());
        }

        return WorkCreateResponse.builder()
                .workId(work.getId())
                .build();
    }


    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long userId, Long workId) {
        // TODO: 상세 조회 로직 구현
        return null;
    }

    private Integer calculateGrossDailyIncome(LocalDateTime startTime, LocalDateTime endTime, int restMinutes, int hourlyRate, boolean hasNightAllowance) {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return 0;
        }

        Duration totalDuration = Duration.between(startTime, endTime);
        long workMinutes = totalDuration.toMinutes() - restMinutes;
        if (workMinutes <= 0) {
            return 0;
        }

        double baseIncome = (double) workMinutes / 60 * hourlyRate;
        double nightAllowance = 0.0;

        if (hasNightAllowance) {
            LocalDateTime nightStart = startTime.toLocalDate().atTime(22, 0);
            LocalDateTime nightEnd = startTime.toLocalDate().plusDays(1).atTime(6, 0);

            // 실제 근무 시간과 야간수당 적용 시간의 겹치는 시간을 계산
            long nightWorkMinutes = 0;
            LocalDateTime cursor = startTime;
            while(cursor.isBefore(endTime)) {
                // 22:00 ~ 06:00 사이인지 확인
                if (!cursor.isBefore(nightStart) && cursor.isBefore(nightEnd)) {
                    nightWorkMinutes++;
                }
                cursor = cursor.plusMinutes(1);
            }

            // 야간수당은 0.5배를 가산
            nightAllowance = ((double) nightWorkMinutes / 60 * hourlyRate) * 0.5;
        }

        return (int) Math.round(baseIncome + nightAllowance);
    }
}
