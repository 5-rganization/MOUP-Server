package com.moup.server.repository;

import com.moup.server.model.entity.Routine;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoutineRepository {

    /// 루틴을 생성하는 메서드.
    ///
    /// @param routine 생성할 루틴 엔티티
    /// @return 생성된 행의 수
    @Insert("INSERT INTO routines (user_id, routine_name, alarm_time) VALUES (#{userId}, #{routineName}, #{alarmTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Routine routine);

    /// 루틴의 ID와 사용자 ID를 통해 해당 루틴이 존재하는지 여부를 반환하는 메서드
    ///
    /// @param id 조회할 루틴 ID
    /// @param userId 조회할 루틴의 사용자 ID
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM routines WHERE id = #{id} AND user_id = #{userId})")
    boolean existByIdAndUserId(Long id, Long userId);

    /// 사용자 ID와 루틴 이름을 통해 해당 루틴이 존재하는지(사용자가 이미 만들었는지) 여부를 반환하는 메서드
    ///
    /// @param userId 조회할 루틴의 사용자 ID
    /// @param routineName 조회할 루틴의 이름
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM routines WHERE user_id = #{userId} AND routine_name = #{routineName})")
    boolean existByUserIdAndRoutineName(Long userId, String routineName);

    /// 루틴의 ID와 사용자 ID를 통해 해당 루틴을 찾고, 그 루틴 객체를 반환하는 메서드
    ///
    /// @param id 조회할 루틴의 ID
    /// @param userId 조회할 루틴의 사용자 ID
    /// @return 조회된 Routine 객체, 없으면 Optional.empty
    @Select("SELECT * FROM routines WHERE id = #{id} AND user_id = #{userId}")
    Optional<Routine> findByIdAndUserId(Long id, Long userId);

    /// 사용자 ID를 통해 해당 사용자의 모든 루틴 객체를 리스트로 반환하는 메서드
    ///
    /// @param userId 조회할 루틴의 사용자 ID
    /// @return 조회된 Routine 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM routines WHERE user_id = #{userId} ORDER BY alarm_time IS NULL ASC, alarm_time ASC")
    List<Routine> findAllByUserId(Long userId);

    /// 루틴의 ID와 사용자 ID에 해당하는 루틴을 업데이트하는 메서드.
    ///
    /// @param routine 업데이트할 루틴 엔티티
    @Update("UPDATE routines SET routine_name = #{routineName}, alarm_time = #{alarmTime} WHERE id = #{id} AND user_id = #{userId}")
    void update(Routine routine);

    /// 루틴의 ID와 사용자 ID에 해당하는 루틴을 삭제하는 메서드.
    ///
    /// @param id 삭제할 루틴의 ID
    /// @param userId 삭제할 루틴의 사용자 ID
    @Delete("DELETE FROM routines WHERE id = #{id} AND user_id = #{userId}")
    void deleteByIdAndUserId(Long id, Long userId);

    /// 루틴과 근무를 연결하는 매핑을 생성합니다.
    ///
    /// @param workId 연결할 근무의 ID
    /// @param routineId 연결할 루틴의 ID
    /// @return 생성된 행의 수
    @Insert("INSERT INTO work_routine_mappings (work_id, routine_id) VALUES (#{workId}, #{routineId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long mappingRoutineToWork(Long routineId, Long workId);

    /// 루틴의 ID와 사용자 ID를 통해 해당 루틴을 찾고, 그 루틴 객체를 반환하는 메서드
    ///
    /// @param workId 조회할 근무의 ID
    /// @return 조회된 Routine 객체, 없으면 Optional.empty
    @Select("SELECT routine_id FROM work_routine_mappings WHERE work_id = #{workId}")
    Optional<Routine> findByWorkId(Long workId);
}
