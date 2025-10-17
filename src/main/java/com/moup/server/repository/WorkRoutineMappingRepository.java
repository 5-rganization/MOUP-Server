package com.moup.server.repository;

import com.moup.server.model.entity.WorkRoutineMapping;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkRoutineMappingRepository {

    /// 여러 개의 Work-Routine 매핑을 한 번에 생성하는 메서드 (배치 삽입)
    ///
    /// @param mappingList 생성할 매핑 객체 리스트
    @Insert("""
            <script>
                INSERT INTO work_routine_mappings (work_id, routine_id)
                VALUES
                <foreach item="mapping" collection="mappingList" separator=",">
                    (#{mapping.workId}, #{mapping.routineId})
                </foreach>
            </script>
            """)
    void createBatch(@Param("mappingList") List<WorkRoutineMapping> mappingList);

    /// 근무 ID를 통해 해당 근무-루틴 매핑을 모두 찾고, 그 배열을 반환하는 메서드
    ///
    /// @param workId 조회할 근무-루틴 매핑의 근무 ID
    /// @return 조회된 `WorkRoutineMapping` 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM work_routine_mappings WHERE work_id = #{workId}")
    List<WorkRoutineMapping> findAllByWorkId(Long workId);

    /// 여러 근무 ID에 해당하는 모든 근무-루틴 매핑을 조회하는 메서드
    ///
    /// @param workIdList 조회할 근무 ID 리스트
    /// @return 조회된 WorkRoutineMapping 객체 리스트
    @Select("""
            <script>
                SELECT * FROM work_routine_mappings
                WHERE work_id IN
                <foreach item="workId" collection="workIdList" open="(" separator="," close=")">
                    #{workId}
                </foreach>
            </script>
            """)
    List<WorkRoutineMapping> findAllByWorkIdListIn(@Param("workIdList") List<Long> workIdList);

    /// 근무 ID에 해당하는 근무-루틴 매핑을 모두 삭제하는 메서드
    ///
    /// @param workId 삭제할 근무-루틴 매핑의 근무 ID
    @Delete("DELETE FROM work_routine_mappings WHERE work_id = #{workId}")
    void deleteByWorkId(Long workId);

    /// 루틴 ID에 해당하는 근무-루틴 매핑을 모두 삭제하는 메서드
    ///
    /// @param routineId 삭제할 근무-루틴 매핑의 루틴 ID
    @Delete("DELETE FROM work_routine_mappings WHERE routine_id = #{routineId}")
    void deleteByRoutineId(Long routineId);
}
