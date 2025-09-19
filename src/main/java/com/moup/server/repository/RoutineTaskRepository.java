package com.moup.server.repository;

import com.moup.server.model.entity.RoutineTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoutineTaskRepository {

    /**
     * 여러 개의 할 일을 한 번에 생성하는 메서드. (Batch Insert)
     *
     * @param routineTaskList 생성할 RoutineTask 객체 리스트
     * @return 생성된 행의 수
     */
    Long createAll(List<RoutineTask> routineTaskList);

    /**
     * 루틴 ID를 통해 해당 루틴의 모든 할 일 객체를 리스트로 반환하는 메서드
     *
     * @param routineId 조회할 할 일의 루틴 ID
     * @return 조회된 RoutineTask 객체 리스트, 없으면 빈 배열
     */
    @Select("SELECT * FROM routine_tasks WHERE routine_id = #{routineId}")
    List<RoutineTask> findAllByRoutineId(Long routineId);

    /**
     * 여러 개의 할 일을 한 번에 업데이트하는 메서드. (Batch Update)
     *
     * @param routineTaskList 업데이트할 RoutineTask 객체 리스트
     */
    void updateTasks(List<RoutineTask> routineTaskList);

    /**
     * 여러 개의 할 일을 한 번에 삭제하는 메서드. (Batch Delete)
     *
     * @param routineId 삭제할 할 일의 루틴 ID
     * @param idList 삭제할 할 일 ID 리스트
     */
    void deleteTasks(@Param("routineId") Long routineId, @Param("idList") List<Long> idList);
}
