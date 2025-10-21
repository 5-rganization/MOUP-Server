package com.moup.server.repository;

import com.moup.server.model.entity.Work;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkRepository {

    /// 근무를 생성하는 메서드
    ///
    /// @param work 생성할 근무 Worker 객체
    /// @return 생성된 행의 수
    @Insert("""
            INSERT INTO works (
                worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time,
                rest_time_minutes, gross_work_minutes, net_work_minutes, memo, hourly_rate, base_pay, night_allowance, holiday_allowance,
                gross_income, estimated_net_income, repeat_days, repeat_end_date
            )
            VALUES (
                #{workerId}, #{workDate}, #{startTime}, #{actualStartTime}, #{endTime}, #{actualEndTime},
                #{restTimeMinutes}, #{grossWorkMinutes}, #{netWorkMinutes}, #{memo}, #{hourlyRate}, #{basePay}, #{nightAllowance}, #{holidayAllowance},
                #{grossIncome}, #{estimatedNetIncome}, #{repeatDays}, #{repeatEndDate}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Work work);

    /// 근무 ID와 근무자 ID를 통해 해당 근무가 존재하는지 여부를 반환하는 메서드
    ///
    /// @param id 조회할 근무 ID
    /// @param workerId 조회할 근무자 ID
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM works WHERE id = #{id} AND worker_id = #{workerId})")
    boolean existsByIdAndWorkerId(Long id, Long workerId);

    /// 근무 ID를 통해 해당 근무를 찾고, 그 근무의 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무 ID
    /// @return 조회된 Work 객체, 없으면 Optional.empty
    @Select("SELECT * FROM works WHERE id = #{id}")
    Optional<Work> findById(Long id);

    /// 근무 ID와 근무자 ID를 통해 해당 근무를 찾고, 그 근무의 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무 ID
    /// @param workerId 조회할 근무자 ID
    /// @return 조회된 Work 객체, 없으면 Optional.empty
    @Select("SELECT * FROM works WHERE id = #{id} AND worker_id = #{workerId}")
    Optional<Work> findByIdAndWorkerId(Long id, Long workerId);

    /// 근무자 ID와 근무 날짜를 통해 해당 날짜의 모든 근무를 조회하는 메서드
    ///
    /// @param workerId 조회할 근무자 ID
    /// @param workDate 조회할 근무 날짜
    /// @return 조회된 Work 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM works WHERE worker_id = #{workerId} AND work_date = #{workDate}")
    List<Work> findAllByWorkerIdAndWorkDate(Long workerId, LocalDate workDate);

    /// 근무자 ID에 해당하는 근무 중에서 특정 기간동안 모든 근무를 조회하는 메서드
    ///
    /// @param workerId 조회할 근무자 ID
    /// @param startDate 조회할 시작일
    /// @param endDate 조회할 마지막일
    /// @return 조회된 Work 객체 리스트
    @Select("SELECT * FROM works WHERE worker_id = #{workerId} AND work_date BETWEEN #{startDate} AND #{endDate} ORDER BY work_date")
    List<Work> findAllByWorkerIdAndDateRange(Long workerId, LocalDate startDate, LocalDate endDate);

    /// 여러 근무자 ID와 기간에 해당하는 모든 근무를 조회하는 메서드
    ///
    /// @param workerIdList 조회할 근무자 ID 리스트
    /// @param startDate 조회할 시작일
    /// @param endDate 조회할 마지막일
    /// @return 조회된 Work 객체 리스트
    @Select("""
            <script>
                SELECT * FROM works
                WHERE work_date BETWEEN #{startDate} AND #{endDate}
                AND worker_id IN
                <foreach item="workerId" collection="workerIdList" open="(" separator="," close=")">
                    #{workerId}
                </foreach>
                ORDER BY work_date
            </script>
            """)
    List<Work> findAllByWorkerIdListInAndDateRange(
            @Param("workerIdList") List<Long> workerIdList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /// 근무 ID와 근무자 ID에 해당하는 근무를 업데이트하는 메서드
    ///
    /// @param work 업데이트할 Work 객체
    @Update("""
            UPDATE works
            SET
                work_date = #{workDate}, start_time = #{startTime}, actual_start_time = #{actualStartTime},
                end_time = #{endTime}, actual_end_time = #{actualEndTime}, rest_time_minutes = #{restTimeMinutes},
                gross_work_minutes = #{grossWorkMinutes}, net_work_minutes = #{netWorkMinutes},
                memo = #{memo}, hourly_rate = #{hourlyRate}, base_pay = #{basePay},
                night_allowance = #{nightAllowance}, holiday_allowance = #{holidayAllowance},
                gross_income = #{grossIncome}, estimated_net_income = #{estimatedNetIncome}, repeat_days = #{repeatDays}, repeat_end_date = #{repeatEndDate}
            WHERE id = #{id} AND worker_id = #{workerId}
            """)
    void update(Work work);

    /// 근무 ID와 근무자 ID에 해당하는 근무를 삭제하는 메서드
    ///
    /// @param id 삭제할 근무의 ID
    /// @param workerId 삭제할 근무의 근무자 ID
    @Delete("DELETE FROM works WHERE id = #{id} AND worker_id = #{workerId}")
    void delete(Long id, Long workerId);
}
