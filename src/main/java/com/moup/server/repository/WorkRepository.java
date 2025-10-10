package com.moup.server.repository;

import com.moup.server.model.entity.Work;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkRepository {

    /// 근무를 생성하는 메서드.
    ///
    /// @param work 생성할 근무 Worker 객체
    /// @return 생성된 행의 수
    @Insert("""
            INSERT INTO works (worker_id, routine_id, work_date, start_time, actual_start_time, end_time, actual_end_time,
                               rest_time, memo, daily_income, repeat_days, repeat_end_date)
            VALUES (#{workerId}, #{routineId}, #{workDate}, #{startTime}, #{actualStartTime}, #{endTime}, #{actualEndTime},
                    #{restTime}, #{memo}, #{dailyIncome}, #{repeatDays}, #{repeatEndDate})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Work work);

    /// 근무 ID를 통해 해당 근무를 찾고, 그 근무의 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무의 ID
    /// @return 조회된 Work 객체, 없으면 Optional.empty
    @Select("SELECT * FROM works WHERE id = #{id}")
    Optional<Work> findById(Long id);

    /// 근무자 ID와 근무 날짜를 통해 해당 근무가 존재하는지 여부를 반환하는 메서드
    ///
    /// @param workerId 조회할 근무자 ID
    /// @param workDate 조회할 근무 날짜
    /// @return 조회된 Work 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM works WHERE worker_id = #{workerId} AND work_date = #{workDate}")
    List<Work> findByWorkerIdAndWorkDate(Long workerId, LocalDate workDate);

    /// 근무 ID와 근무자 ID에 해당하는 근무를 업데이트하는 메서드.
    ///
    /// @param work 업데이트할 Work 객체
    @Update("""
            UPDATE works
            SET work_date = #{workDate}, start_time = #{startTime}, actual_start_time = #{actualStartTime}, end_time = #{endTime}, actual_end_time = #{actualEndTime},
                rest_time = #{restTime}, memo = #{memo}, daily_income = #{dailyIncome}, repeat_days = #{repeatDays}, repeat_end_date = #{repeatEndDate}
            WHERE id = #{id} AND worker_id = #{workerId}
            """)
    void update(Work work);

    /// 근무 ID와 근무자 ID에 해당하는 근무를 삭제하는 메서드.
    ///
    /// @param id 삭제할 근무의 ID
    /// @param workerId 삭제할 근무의 근무자 ID
    @Delete("DELETE FROM works WHERE id = #{id} AND worker_id = #{workerId}")
    void deleteByIdAndWorkerId(Long id, Long workerId);
}
