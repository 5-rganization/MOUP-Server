package com.moup.domain.routine.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RoutineTaskCompletionRepository {

    /// 체크. `UNIQUE (work_id, routine_task_id)`와 짝을 이뤄 **멱등**하다 —
    /// 더블탭이나 네트워크 재시도로 같은 요청이 두 번 와도 중복 행이 생기지 않는다.
    @Insert("""
            INSERT INTO routine_task_completions (work_id, routine_task_id)
            VALUES (#{workId}, #{routineTaskId})
            ON DUPLICATE KEY UPDATE work_id = VALUES(work_id)
            """)
    void complete(@Param("workId") Long workId, @Param("routineTaskId") Long routineTaskId);

    /// 체크 해제. 없는 행을 지워도 무해하다.
    @Delete("DELETE FROM routine_task_completions WHERE work_id = #{workId} AND routine_task_id = #{routineTaskId}")
    void uncomplete(@Param("workId") Long workId, @Param("routineTaskId") Long routineTaskId);

    /// 이 근무에서 완료된 할 일 id들.
    @Select("SELECT routine_task_id FROM routine_task_completions WHERE work_id = #{workId}")
    List<Long> findCompletedTaskIdsByWorkId(Long workId);

    // ===== 반복 근무 교체 시 완료 상태 보존 =====
    //
    // `replaceWithNewRecurringWorks`가 기존 근무를 지우고 새로 만든다. 그러면
    // `work_id` CASCADE로 **체크가 전부 날아간다.** 근무를 실제로 지운 것이 아니라
    // 같은 날의 근무를 다시 만든 것뿐인데도 그렇다.
    //
    // 삭제 **직전에** (근무일, 할 일)로 떠 두었다가 재생성 후 같은 날짜의 새 근무에 붙인다.
    // 조회 범위를 실제 삭제 대상으로 좁히는 것이 중요하다 — 지워지지 않는 근무의 체크까지
    // 퍼오면, 같은 날짜에 근무가 둘일 때 엉뚱한 근무에 체크가 붙는다.

    record CompletionSnapshot(LocalDate workDate, Long routineTaskId) {}

    /// 반복 그룹에서 기준일 이후로 삭제될 근무들의 체크 상태.
    @Select("""
            SELECT w.work_date AS workDate, c.routine_task_id AS routineTaskId
            FROM routine_task_completions c
            JOIN works w ON w.id = c.work_id
            WHERE w.repeat_group_id = #{repeatGroupId} AND w.work_date >= #{fromDate}
            """)
    List<CompletionSnapshot> findSnapshotsByRepeatGroupFrom(
            @Param("repeatGroupId") String repeatGroupId, @Param("fromDate") LocalDate fromDate);

    /// 반복이 아니던 단일 근무 하나의 체크 상태.
    @Select("""
            SELECT w.work_date AS workDate, c.routine_task_id AS routineTaskId
            FROM routine_task_completions c
            JOIN works w ON w.id = c.work_id
            WHERE c.work_id = #{workId}
            """)
    List<CompletionSnapshot> findSnapshotsByWorkId(Long workId);
}
