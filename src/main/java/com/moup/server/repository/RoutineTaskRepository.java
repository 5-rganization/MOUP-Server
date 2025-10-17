package com.moup.server.repository;

import com.moup.server.model.entity.RoutineTask;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoutineTaskRepository {

    /// 할 일을 생성하는 메서드
    ///
    /// @param routineTask 생성할 RoutineTask 객체
    /// @return 생성된 행의 수
    @Insert("INSERT INTO routine_tasks (routine_id, content, order_index) VALUES (#{routineId}, #{content}, #{orderIndex})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(RoutineTask routineTask);

    /// 루틴 ID를 통해 해당 루틴의 모든 할 일 객체를 리스트로 반환하는 메서드
    ///
    /// @param routineId 조회할 할 일의 루틴 ID
    /// @return 조회된 RoutineTask 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM routine_tasks WHERE routine_id = #{routineId} ORDER BY order_index")
    List<RoutineTask> findAllByRoutineId(Long routineId);

    /// 루틴 ID에 해당하는 할 일들을 모두 삭제하는 메서드
    ///
    /// @param routineId 삭제할 할 일들의 루틴 ID
    @Delete("DELETE FROM routine_tasks WHERE routine_id = #{routineId}")
    void deleteAllByRoutineId(Long routineId);
}
