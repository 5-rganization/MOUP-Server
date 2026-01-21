package com.moup.server.repository;

import com.moup.server.model.entity.Work;
import java.util.Map;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkRepository {

  @Select("""
      <select id="findAllByWorkIdIn" resultType="Work">
          SELECT * FROM work
          WHERE work_id IN
          <foreach item="id" collection="list" open="(" separator="," close=")">
              #{id}
          </foreach>
      </select>
      """)
  List<Work> findAllByIdIn(List<Long> workIdList);

  /// 근무를 생성하는 메서드
  ///
  /// @param work 생성할 근무 Worker 객체
  /// @return 생성된 행의 수
  @Insert("""
      INSERT INTO works (
          worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time,
          rest_time_minutes, gross_work_minutes, net_work_minutes, night_work_minutes,
          memo, hourly_rate, base_pay, night_allowance, holiday_allowance,
          gross_income, estimated_net_income, repeat_group_id
      )
      VALUES (
          #{workerId}, #{workDate}, #{startTime}, #{actualStartTime}, #{endTime}, #{actualEndTime},
          #{restTimeMinutes}, #{grossWorkMinutes}, #{netWorkMinutes}, #{nightWorkMinutes},
          #{memo}, #{hourlyRate}, #{basePay}, #{nightAllowance}, #{holidayAllowance},
          #{grossIncome}, #{estimatedNetIncome}, #{repeatGroupId}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  long create(Work work);

  /// 반복 근무 생성 시 배치(Batch) 삽입을 위한 메서드
  @Insert("""
      <script>
          INSERT INTO works (
              worker_id, work_date, start_time, end_time, rest_time_minutes,
              gross_work_minutes, net_work_minutes, night_work_minutes,
              memo, hourly_rate, base_pay, night_allowance, holiday_allowance,
              gross_income, estimated_net_income, repeat_group_id
          )
          VALUES
          <foreach item="work" collection="works" separator=",">
              (
                  #{work.workerId}, #{work.workDate}, #{work.startTime}, #{work.endTime}, #{work.restTimeMinutes},
                  #{work.grossWorkMinutes}, #{work.netWorkMinutes}, #{work.nightWorkMinutes},
                  #{work.memo}, #{work.hourlyRate}, #{work.basePay}, #{work.nightAllowance}, #{work.holidayAllowance},
                  #{work.grossIncome}, #{work.estimatedNetIncome}, #{work.repeatGroupId}
              )
          </foreach>
      </script>
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  long createBatch(@Param("works") List<Work> works);

  /// 근무 ID와 근무자 ID를 통해 해당 근무가 존재하는지 여부를 반환하는 메서드
  ///
  /// @param id       조회할 근무 ID
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
  /// @param id       조회할 근무 ID
  /// @param workerId 조회할 근무자 ID
  /// @return 조회된 Work 객체, 없으면 Optional.empty
  @Select("SELECT * FROM works WHERE id = #{id} AND worker_id = #{workerId}")
  Optional<Work> findByIdAndWorkerId(Long id, Long workerId);

  /// 근무자 ID를 통해 해당 근무자의 모든 근무를 조회하는 메서드 (`start_time` 내림차순)
  ///
  /// @param workerId 조회할 근무자 ID
  /// @return 조회된 Work 객체 리스트, 없으면 빈 배열
  @Select("SELECT * FROM works WHERE worker_id = #{workerId} ORDER BY start_time DESC")
  List<Work> findAllByWorkerId(Long workerId);

  /// 근무자 ID와 근무 날짜를 통해 해당 날짜의 모든 근무를 조회하는 메서드
  ///
  /// @param workerId 조회할 근무자 ID
  /// @param workDate 조회할 근무 날짜
  /// @return 조회된 Work 객체 리스트, 없으면 빈 배열
  @Select("SELECT * FROM works WHERE worker_id = #{workerId} AND work_date = #{workDate}")
  List<Work> findAllByWorkerIdAndWorkDate(Long workerId, LocalDate workDate);

  /// 근무자 ID에 해당하는 근무 중에서 특정 기간동안 모든 근무를 조회하는 메서드 (`work_date` 오름차순)
  ///
  /// @param workerId  조회할 근무자 ID
  /// @param startDate 조회할 시작일
  /// @param endDate   조회할 마지막일
  /// @return 조회된 Work 객체 리스트
  @Select("SELECT * FROM works WHERE worker_id = #{workerId} AND work_date BETWEEN #{startDate} AND #{endDate} ORDER BY work_date")
  List<Work> findAllByWorkerIdAndDateRange(Long workerId, LocalDate startDate, LocalDate endDate);

  /// 여러 근무자 ID와 기간에 해당하는 모든 근무를 조회하는 메서드 (`work_date` 오름차순)
  ///
  /// @param workerIdList 조회할 근무자 ID 리스트
  /// @param startDate    조회할 시작일
  /// @param endDate      조회할 마지막일
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

  /// 특정 근무자(`workerId`)의 근무 중,
  /// 1. 현재 시간(`currentDateTime`) 기준으로 1시간 전 ~ 1시간 후 사이에 시작하고 (`start_time`)
  /// 2. 아직 '실제 출근'을 기록하지 않았으며 (`actual_start_time IS NULL`)
  /// 3. 아직 '실제 퇴근'도 기록되지 않은 (`actual_end_time IS NULL`) 가장 빠른 근무 1건을 조회하는 메서드 (`start_time` 오름차순)
  ///
  /// @param workerId        조회할 근무자 ID
  /// @param currentDateTime 기준이 되는 현재 시간
  /// @return 조회된 `Work` 객체, 없으면 `Optional.empty`
  @Select("""
      SELECT * FROM works
      WHERE worker_id = #{workerId}
          AND actual_start_time IS NULL
          AND actual_end_time IS NULL
          AND start_time BETWEEN DATE_SUB(#{currentDateTime}, INTERVAL 1 HOUR) AND DATE_ADD(#{currentDateTime}, INTERVAL 1 HOUR)
      ORDER BY start_time
      LIMIT 1
      """)
  Optional<Work> findEligibleWorkForClockIn(
      @Param("workerId") Long workerId,
      @Param("currentDateTime") LocalDateTime currentDateTime
  );

  /// 특정 근무자(`workerId`)의 근무 중, 실제 출근이 기록되었고 (`actual_start_time IS NOT NULL`) 실제 퇴근은 기록되지 않은
  /// (`actual_end_time IS NULL`) 근무를 '실제 출근 시간' 기준으로 가장 최근 1건을 조회하는 메서드 (`actual_start_time` 내림차순)
  /// - 현재 진행 중인 근무를 찾을 때 사용
  ///
  /// @param workerId 조회할 근무자 ID
  /// @return 조회된 `Work` 객체, 없으면 `Optional.empty`
  @Select("""
      SELECT * FROM works
      WHERE worker_id = #{workerId}
          AND actual_start_time IS NOT NULL
          AND actual_end_time IS NULL
      ORDER BY actual_start_time DESC
      LIMIT 1
      """)
  Optional<Work> findMostRecentWorkInProgress(@Param("workerId") Long workerId);

  /// 특정 반복 그룹 ID에 해당하는 근무 중 가장 빠른 날짜의 근무를 조회합니다. (반복 시작일 및 요일 계산 기준 확인용)
  ///
  /// @param repeatGroupId 반복 그룹 ID
  /// @return 가장 빠른 근무 Optional
  @Select("SELECT * FROM works WHERE repeat_group_id = #{repeatGroupId} ORDER BY work_date , start_time LIMIT 1")
  Optional<Work> findFirstWorkByRepeatGroupId(@Param("repeatGroupId") String repeatGroupId);

  /// 특정 반복 그룹 ID에 해당하는 근무 중 가장 늦은 날짜(반복 종료일)를 조회합니다.
  ///
  /// @param repeatGroupId 반복 그룹 ID
  /// @return 반복 종료 날짜 Optional
  @Select("SELECT MAX(work_date) FROM works WHERE repeat_group_id = #{repeatGroupId}")
  Optional<LocalDate> findLastWorkDateByRepeatGroupId(@Param("repeatGroupId") String repeatGroupId);

  /// 특정 반복 그룹 ID에 해당하는 근무들의 요일(DayOfWeek) 목록을 중복 없이 조회합니다. (반복 요일 확인용)
  ///
  /// @param repeatGroupId 반복 그룹 ID
  /// @return 요일(DayOfWeek) 이름 문자열 목록 (e.g., ["MONDAY", "WEDNESDAY"])
  @Select("SELECT DISTINCT DAYNAME(work_date) FROM works WHERE repeat_group_id = #{repeatGroupId}")
  List<String> findDistinctDayNamesByRepeatGroupId(@Param("repeatGroupId") String repeatGroupId);

  /// 여러 반복 그룹 ID들에 해당하는 근무 중 가장 늦은 날짜(반복 종료일) 목록을 조회합니다.
  ///
  /// @param groupIdList 반복 그룹 ID 리스트
  /// @return 각 그룹 ID와 해당 그룹의 마지막 근무 날짜(lastDate)를 담은 리스트
  @Select("""
      <script>
      SELECT repeat_group_id as groupId, MAX(work_date) as lastDate
      FROM works
      WHERE repeat_group_id IN
          <foreach item="id" collection="groupIdList" open="(" separator="," close=")">
              #{id}
          </foreach>
      GROUP BY repeat_group_id
      </script>
      """)
  List<GroupIdAndDate> findLastWorkDatesByGroupIdList(
      @Param("groupIdList") Collection<String> groupIdList);

  /// 여러 반복 그룹 ID들에 해당하는 근무들의 요일(DayOfWeek) 이름 목록을 조회합니다. (GROUP BY를 사용하여 각 그룹별 요일 조합을 가져옵니다)
  ///
  /// @param groupIdList 반복 그룹 ID 리스트
  /// @return 각 그룹 ID와 해당 그룹에 포함된 요일 이름(dayName)을 담은 리스트 (중복될 수 있음)
  @Select("""
      <script>
      SELECT repeat_group_id as groupId, DAYNAME(work_date) as dayName
      FROM works
      WHERE repeat_group_id IN
          <foreach item="id" collection="groupIdList" open="(" separator="," close=")">
              #{id}
          </foreach>
      GROUP BY repeat_group_id, DAYNAME(work_date) /* 각 그룹 내 요일 중복 제거 */
      ORDER BY repeat_group_id /* Java 처리 용이성을 위해 정렬 */
      </script>
      """)
  List<GroupIdAndDayName> findDistinctDayNamesByGroupIdList(
      @Param("groupIdList") Collection<String> groupIdList);

  /// 특정 근무자의 특정 날짜(startDate) 이후의 '고유한 근무 연/월' 목록을 조회합니다.
  ///
  /// @param workerId  조회할 근무자 ID
  /// @param startDate 조회를 시작할 날짜
  @Select("""
      SELECT DISTINCT YEAR(work_date) AS year, MONTH(work_date) AS month
      FROM works
      WHERE worker_id = #{workerId}
          AND work_date >= #{startDate}
      ORDER BY year, month
      """)
  List<WorkMonthDto> findDistinctWorkMonthsAfter(
      @Param("workerId") Long workerId,
      @Param("startDate") LocalDate startDate
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
          night_work_minutes = #{nightWorkMinutes},
          memo = #{memo}, hourly_rate = #{hourlyRate}, base_pay = #{basePay},
          night_allowance = #{nightAllowance}, holiday_allowance = #{holidayAllowance},
          gross_income = #{grossIncome}, estimated_net_income = #{estimatedNetIncome}, 
          repeat_group_id = #{repeatGroupId}
      WHERE id = #{id} AND worker_id = #{workerId}
      """)
  void update(Work work);

  /// 근무 ID에 해당하는 근무의 실제 출근 시간을 업데이트하는 메서드
  ///
  /// @param actualStartTime 업데이트할 실제 출근 시간
  @Update("UPDATE works SET actual_start_time = #{actualStartTime} WHERE id = #{id}")
  void updateActualStartTimeById(Long id, LocalDateTime actualStartTime);

  /// 근무 ID에 해당하는 근무의 실제 퇴근 시간을 업데이트하는 메서드 'end_time'(예정 퇴근 시간)이 비어있을(`NULL`) 경우, 'end_time'도
  /// 'actual_end_time'과 동일한 값으로 함께 업데이트합니다.
  ///
  /// @param id            업데이트할 근무 ID
  /// @param actualEndTime 업데이트할 실제 퇴근 시간 (LocalDateTime)
  @Update("UPDATE works SET actual_end_time = #{actualEndTime}, end_time = COALESCE(end_time, #{actualEndTime}) WHERE id = #{id}")
  void updateActualEndTimeById(@Param("id") Long id,
      @Param("actualEndTime") LocalDateTime actualEndTime);

  /// 특정 근무자의 특정 기간 동안의 모든 근무 기록에 대해 '추정 세후 일급(estimated_net_income)'을 0으로 일괄 업데이트합니다.
  @Update("""
      UPDATE works
      SET estimated_net_income = 0
      WHERE worker_id = #{workerId}
          AND work_date BETWEEN #{startDate} AND #{endDate}
      """)
  void updateEstimatedNetIncomeToZeroByDateRange(
      @Param("workerId") Long workerId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  /// 특정 근무자의 특정 기간 동안의 모든 근무 기록에 대해 '일일 추정 공제액'을 바탕으로 '추정 세후 일급'을 일괄 업데이트합니다. (GREATEST 함수는 0 미만이
  /// 되는 것을 방지합니다)
  @Update("""
      UPDATE works
      SET estimated_net_income = GREATEST(0, gross_income - #{dailyDeduction})
      WHERE worker_id = #{workerId}
          AND work_date BETWEEN #{startDate} AND #{endDate}
      """)
  void updateAllEstimatedNetIncomesForMonth(
      @Param("workerId") Long workerId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("dailyDeduction") int dailyDeduction
  );

  /**
   * 주휴수당 재계산 등으로 변경된 주(Week) 단위의 근무 상세 내역을 (basePay, nightAllowance 등) '배치 업데이트'합니다. (MyBatis
   * <foreach>와 SQL CASE 문을 사용)
   *
   * @param worksToUpdate 업데이트할 Work 객체 리스트
   */
  @Update("""
      <script>
          UPDATE works
          SET
              gross_work_minutes =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.grossWorkMinutes}
                  </foreach>,
              net_work_minutes =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.netWorkMinutes}
                  </foreach>,
              night_work_minutes =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.nightWorkMinutes}
                  </foreach>,
              base_pay =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.basePay}
                  </foreach>,
              night_allowance =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.nightAllowance}
                  </foreach>,
              holiday_allowance =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.holidayAllowance}
                  </foreach>,
              gross_income =
                  <foreach item='work' collection='worksToUpdate' open='CASE id' close=' END'>
                      WHEN #{work.id} THEN #{work.grossIncome}
                  </foreach>
          WHERE id IN
              <foreach item='work' collection='worksToUpdate' open='(' separator=',' close=')'>
                  #{work.id}
              </foreach>
      </script>
      """)
  void updateWorkWeekDetailsBatch(@Param("worksToUpdate") List<Work> worksToUpdate);

  /// 근무 ID와 근무자 ID에 해당하는 근무를 삭제하는 메서드
  ///
  /// @param id       삭제할 근무의 ID
  /// @param workerId 삭제할 근무의 근무자 ID
  @Delete("DELETE FROM works WHERE id = #{id} AND worker_id = #{workerId}")
  void delete(Long id, Long workerId);

  /// 특정 반복 그룹(`repeatGroupId`)에 속하면서 특정 날짜(`date`) **포함** 이후의 모든 근무를 삭제합니다.
  ///
  /// @param repeatGroupId 삭제할 반복 그룹 ID
  /// @param date          기준 날짜 (이 날짜 포함 미래의 근무 삭제)
  /// @return 삭제된 행의 수
  @Delete("""
      DELETE FROM works
      WHERE repeat_group_id = #{repeatGroupId}
          AND work_date >= #{date} /* 기준일 포함 */
      """)
  int deleteRecurringWorkFromDate(@Param("repeatGroupId") String repeatGroupId,
      @Param("date") LocalDate date);

  /// 특정 반복 그룹(`repeatGroupId`)에 속하면서 특정 날짜(`date`) **보다 미래**의 모든 근무를 삭제합니다.
  ///
  /// @param repeatGroupId 삭제할 반복 그룹 ID
  /// @param date          기준 날짜 (이 날짜보다 미래의 근무만 삭제)
  /// @return 삭제된 행의 수
  @Delete("""
      DELETE FROM works
      WHERE repeat_group_id = #{repeatGroupId}
          AND work_date > #{date} /* 기준일 제외 */
      """)
  int deleteRecurringWorkAfterDate(@Param("repeatGroupId") String repeatGroupId,
      @Param("date") LocalDate date);

  @Select("""
          SELECT repeat_group_id, DAYNAME(work_date) as day_name
          FROM works
          WHERE repeat_group_id IN #{groupIds}
          GROUP BY repeat_group_id, day_name
      """)
  Map<String, List<String>> findDayNamesMapByGroupIdsIn(List<String> groupIds);

  // 여러 그룹 ID와 마지막 근무일을 담을 내부 레코드 (또는 DTO)
  record GroupIdAndDate(String groupId, LocalDate lastDate) {

  }

  // 여러 그룹 ID와 요일 이름을 담을 내부 레코드 (또는 DTO)
  record GroupIdAndDayName(String groupId, String dayName) {

  }

  // 쿼리 결과를 매핑할 간단한 DTO
  record WorkMonthDto(int year, int month) {

  }
}
