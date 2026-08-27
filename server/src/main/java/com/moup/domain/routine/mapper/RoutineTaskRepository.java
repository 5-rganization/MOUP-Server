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
}
