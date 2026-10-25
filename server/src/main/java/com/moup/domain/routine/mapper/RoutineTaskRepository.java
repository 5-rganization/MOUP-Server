package com.moup.domain.routine.mapper;

import com.moup.domain.routine.domain.RoutineTask;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoutineTaskRepository {

    /// 여러 개의 할 일을 한 번에 생성하는 메서드 (배치 삽입)
    ///
    /// @param taskList 생성할 RoutineTask 객체 리스트
    @Insert("""
            <script>
                INSERT INTO routine_tasks (routine_id, content, order_index)
                VALUES
                <foreach item="task" collection="taskList" separator=",">
                    (#{task.routineId}, #{task.content}, #{task.orderIndex})
                </foreach>
            </script>
            """)
    void createBatch(@Param("taskList") List<RoutineTask> taskList);

    /// 루틴 ID를 통해 해당 루틴의 모든 할 일 객체를 리스트로 반환하는 메서드 (`order_index` 오름차순)
    ///
    /// @param routineId 조회할 할 일의 루틴 ID
    /// @return 조회된 RoutineTask 객체 리스트, 없으면 빈 배열
    /// 할 일 하나를 조회한다. 체크 요청이 이 근무에 연결된 루틴의 할 일인지 확인할 때 쓴다.
    @Select("SELECT * FROM routine_tasks WHERE id = #{id}")
    Optional<RoutineTask> findById(Long id);

    @Select("SELECT * FROM routine_tasks WHERE routine_id = #{routineId} ORDER BY order_index")
    List<RoutineTask> findAllByRoutineId(Long routineId);

    /// 루틴 ID에 해당하는 할 일들을 모두 삭제하는 메서드
    ///
    /// @param routineId 삭제할 할 일들의 루틴 ID
    @Delete("DELETE FROM routine_tasks WHERE routine_id = #{routineId}")
    void deleteAllByRoutineId(Long routineId);

    /// 루틴의 할 일 중 `keepIds`에 없는 것을 지운다. `keepIds`가 비면 전부 지운다.
    @Delete("""
            <script>
                DELETE FROM routine_tasks WHERE routine_id = #{routineId}
                <if test="keepIds != null and !keepIds.isEmpty()">
                    AND id NOT IN
                    <foreach item="id" collection="keepIds" open="(" separator="," close=")">#{id}</foreach>
                </if>
            </script>
            """)
    void deleteAllByRoutineIdExcept(@Param("routineId") Long routineId, @Param("keepIds") List<Long> keepIds);

    /// 남은 할 일의 정렬 순서를 음수로 비켜 둔다. 순서를 맞바꾸는 수정이 `UNIQUE (routine_id, order_index)`에
    /// 중간에 걸리지 않게 하려는 것이다(0 → -1, 1 → -2 …, 서로 겹치지 않고 새 순서(0 이상)와도 겹치지 않는다).
    @Update("UPDATE routine_tasks SET order_index = -order_index - 1 WHERE routine_id = #{routineId}")
    void moveOrderIndexesOutOfTheWay(@Param("routineId") Long routineId);

    @Update("UPDATE routine_tasks SET content = #{content}, order_index = #{orderIndex} WHERE id = #{id} AND routine_id = #{routineId}")
    void updateContentAndOrder(@Param("id") Long id, @Param("routineId") Long routineId,
                               @Param("content") String content, @Param("orderIndex") Integer orderIndex);

    /// 여러 루틴의 할 일을 한 번에 (루틴 ID, `order_index` 순).
    @Select("""
            <script>
                SELECT * FROM routine_tasks WHERE routine_id IN
                <foreach item="id" collection="routineIds" open="(" separator="," close=")">#{id}</foreach>
                ORDER BY routine_id, order_index
            </script>
            """)
    List<RoutineTask> findAllByRoutineIdIn(@Param("routineIds") List<Long> routineIds);
}
