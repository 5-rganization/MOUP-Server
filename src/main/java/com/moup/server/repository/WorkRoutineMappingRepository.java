package com.moup.server.repository;

import com.moup.server.model.entity.WorkRoutineMapping;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkRoutineMappingRepository {

    /// 근무와 루틴을 연결하는 매핑을 생성하는 메서드
    ///
    /// @param workRoutineMapping 생성할 근무-루틴 매핑 엔티티
    @Insert("INSERT INTO work_routine_mappings (work_id, routine_id) VALUES (#{workId}, #{routineId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void create(WorkRoutineMapping workRoutineMapping);

    /// 근무 ID를 통해 해당 근무-루틴 매핑을 모두 찾고, 그 배열을 반환하는 메서드
    ///
    /// @param workId 조회할 근무-루틴 매핑의 근무 ID
    /// @return 조회된 `WorkRoutineMapping` 객체 리스트, 없으면 빈 배열
    @Select("""
            SELECT * FROM routines
            JOIN work_routine_mappings ON routines.id = work_routine_mappings.routine_id
            WHERE work_routine_mappings.work_id = #{workId}
            """)
    List<WorkRoutineMapping> findAllByWorkId(Long workId);

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
