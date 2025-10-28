package com.moup.server.repository;

import com.moup.server.model.entity.Worker;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkerRepository {

    /// 근무자를 생성하는 메서드.
    ///
    /// @param worker 생성할 Worker 객체
    /// @return 생성된 행의 수
    @Insert("INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted, is_now_working) VALUES (#{userId}, #{workplaceId}, #{workerBasedLabelColor}, #{ownerBasedLabelColor}, #{isAccepted}, #{isNowWorking})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Worker worker);

    /// 근무지 ID와 사용자 ID를 통해 해당 근무지 사용자가 존재하는지 여부를 반환하는 메서드
    ///
    /// @param userId 조회할 사용자 ID
    /// @param workplaceId 조회할 근무지 ID
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM workers WHERE user_id = #{userId} AND workplace_id = #{workplaceId})")
    boolean existsByUserIdAndWorkplaceId(Long userId, Long workplaceId);

    /// 특정 사용자가 현재 근무 중(`is_now_working = true`)인 근무지(`Worker`)가 하나라도 존재하는지 확인하는 메서드
    ///
    /// @param userId 조회할 사용자 ID
    /// @param isNowWorking 확인할 근무 상태 (보통 `true`)
    /// @return 1건이라도 존재하면 `true`, 아니면 `false`
    @Select("""
            SELECT EXISTS(
                SELECT 1
                FROM workers
                WHERE user_id = #{userId} AND is_now_working = #{isNowWorking}
            )
            """)
    boolean existsByUserIdAndIsNowWorking(
            @Param("userId") Long userId,
            @Param("isNowWorking") boolean isNowWorking
    );

    /// 근무자 ID를 통해 해당 사용자의 근무자 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무자 ID
    /// @return 조회된 Worker 객체, 없으면 Optional.empty
    @Select("SELECT * FROM workers WHERE id = #{id}")
    Optional<Worker> findById(Long id);

    /// 근무자 ID와 사용자 ID를 통해 해당 사용자의 근무자 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무자 ID
    /// @param userId 조회할 사용자 ID
    /// @return 조회된 Worker 객체, 없으면 Optional.empty
    @Select("SELECT * FROM workers WHERE id = #{id} AND user_id = #{userId}")
    Optional<Worker> findByIdAndUserId(Long id, Long userId);

    /// 근무자 ID와 근무지 ID를 통해 해당 사용자의 근무자 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무자 ID
    /// @param workplaceId 조회할 근무지 ID
    /// @return 조회된 Worker 객체, 없으면 Optional.empty
    @Select("SELECT * FROM workers WHERE id = #{id} AND workplace_id = #{workplaceId}")
    Optional<Worker> findByIdAndWorkplaceId(Long id, Long workplaceId);

    /// 근무지 ID와 사용자 ID를 통해 해당 근무지 사용자의 근무자 객체를 반환하는 메서드
    ///
    /// @param userId 조회할 사용자 ID
    /// @param workplaceId 조회할 근무지 ID
    /// @return 조회된 Worker 객체, 없으면 Optional.empty
    @Select("SELECT * FROM workers WHERE user_id = #{userId} AND workplace_id = #{workplaceId}")
    Optional<Worker> findByUserIdAndWorkplaceId(Long userId, Long workplaceId);

    /// 사용자 ID를 통해 해당 사용자의 모든 근무자 객체를 리스트로 반환하는 메서드
    ///
    /// @param userId 조회할 사용자 ID
    /// @return 조회된 Worker 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM workers WHERE user_id = #{userId}")
    List<Worker> findAllByUserId(Long userId);

    /// 근무지 ID를 통해 해당 근무지의 모든 근무자 객체를 리스트로 반환하는 메서드
    ///
    /// @param workplaceId 조회할 근무지 ID
    /// @return 조회된 Worker 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM workers WHERE workplace_id = #{workplaceId} ")
    List<Worker> findAllByWorkplaceId(Long workplaceId);

    @Select("""
            <script>
                SELECT * FROM workers
                WHERE workplace_id IN
                <foreach item='id' collection='workplaceIdList' open='(' separator=',' close=')'>
                    #{id}
                </foreach>
            </script>
            """)
    List<Worker> findAllByWorkplaceIdListIn(@Param("workplaceIdList") List<Long> workplaceIdList);

    /// 근무지 ID와 제외할 사용자 ID를 통해 해당 근무지의 근무자 리스트를 반환하는 메서드 (특정 사용자 제외)
    ///
    /// @param workplaceId 조회할 근무지 ID
    /// @param excludeUserId 제외할 사용자 ID
    /// @return 조회된 Worker 객체 리스트, 없으면 빈 배열
    @Select("SELECT * FROM workers WHERE workplace_id = #{workplaceId} AND user_id != #{excludeUserId}")
    List<Worker> findAllByWorkplaceIdAndUserIdNot(Long workplaceId, Long excludeUserId);


    /// 근무자 ID, 사용자 ID, 근무지 ID에 해당하는 근무자의 근무자 기준 라벨 색상을 업데이트하는 메서드.
    ///
    /// @param id 업데이트할 근무자의 ID
    /// @param workplaceId 업데이트할 근무자의 근무지 ID
    /// @param workerBasedLabelColor 업데이트할 라벨 색상
    @Update("UPDATE workers SET worker_based_label_color = #{workerBasedLabelColor} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateWorkerBasedLabelColor(Long id, Long userId, Long workplaceId, String workerBasedLabelColor);

    /// ID에 해당하는 근무자의 사장님 기준 라벨 색상을 업데이트하는 메서드.
    ///
    /// @param id 업데이트할 근무자의 ID
    /// @param workplaceId 업데이트할 근무자의 근무지 ID
    /// @param ownerBasedLabelColor 업데이트할 라벨 색상
    @Update("UPDATE workers SET owner_based_label_color = #{ownerBasedLabelColor} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateOwnerBasedLabelColor(Long id, Long userId, Long workplaceId, String ownerBasedLabelColor);

    /// 근무자 ID, 사용자 ID, 근무지 ID에 해당하는 근무자의 초대 승인 여부를 업데이트하는 메서드.
    ///
    /// @param id 업데이트할 근무자의 ID
    /// @param userId 업데이트할 근무자의 사용자 ID
    /// @param workplaceId 업데이트할 근무자의 근무지 ID
    /// @param isAccepted 업데이트할 초대 승인 여부
    @Update("UPDATE workers SET is_accepted = #{isAccepted} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateIsAccepted(Long id, Long userId, Long workplaceId, Boolean isAccepted);

    /// 근무자 ID, 사용자 ID, 근무지 ID에 해당하는 근무자의 현재 근무 중 여부를 업데이트하는 메서드.
    ///
    /// @param id 업데이트할 근무자의 ID
    /// @param userId 업데이트할 근무자의 사용자 ID
    /// @param workplaceId 업데이트할 근무자의 근무지 ID
    /// @param isNowWorking 업데이트할 근무자의 현재 근무 중 여부
    @Update("UPDATE workers SET is_now_working = #{isNowWorking} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateIsNowWorking(Long id, Long userId, Long workplaceId, Boolean isNowWorking);

    /// 근무자 ID, 사용자 ID, 근무지 ID에 해당하는 근무자를 삭제하는 메서드.
    ///
    /// @param id 삭제할 근무자의 ID
    /// @param userId 삭제할 근무자의 사용자 ID
    /// @param workplaceId 삭제할 근무자의 근무지 ID
    @Delete("DELETE FROM workers WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void delete(Long id, Long userId, Long workplaceId);
}
