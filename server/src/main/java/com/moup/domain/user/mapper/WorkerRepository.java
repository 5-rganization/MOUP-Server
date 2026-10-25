package com.moup.domain.user.mapper;

import com.moup.domain.user.domain.Worker;
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

    /// 근무자 ID와 근무지 ID를 통해 해당 근무지에 근무자가 존재하는지 여부를 반환하는 메서드
    ///
    /// @param id 조회할 근무자 ID
    /// @param workplaceId 조회할 근무지 ID
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM workers WHERE id = #{id} AND workplace_id = #{workplaceId})")
    boolean existsByIdAndWorkplaceId(Long id, Long workplaceId);

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

    /// 사용자의 근무자 행 전부에 쓰기 잠금을 건다(트랜잭션이 끝날 때까지).
    ///
    /// 출근은 "근무 중인 근무지가 없는지 확인 → 근무 중으로 표시"인데 둘 사이에 잠금이 없어, 출근을 두 번
    /// 동시에 누르면(또는 두 근무지에서 동시에) 둘 다 확인을 통과해 근무 중이 2건이 됐다(스코프 2 M6).
    /// 같은 사용자의 출근 요청을 이 잠금으로 줄 세운다. 근무지가 달라도 막아야 하므로 사용자 단위로 잡는다.
    @Select("SELECT id FROM workers WHERE user_id = #{userId} ORDER BY id FOR UPDATE")
    List<Long> lockAllByUserId(@Param("userId") Long userId);

    /// 지금 참여 중인(근무가 끝나지 않은) 근무지 수. 자기가 만든 근무지의 사장님 행도 센다.
    @Select("SELECT COUNT(*) FROM workers w JOIN workplaces p ON p.id = w.workplace_id "
            + "WHERE w.user_id = #{userId} AND w.left_at IS NULL AND (p.owner_id IS NULL OR p.owner_id != #{userId})")
    int countJoinedWorkplacesByUserId(@Param("userId") Long userId);

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

    /// ID 리스트에 포함되고 특정 근무지에 소속된 모든 Worker를 한 번에 조회합니다.
    /// (배치 생성 시 권한 검증용)
    ///
    /// @param workerIdList 조회할 Worker ID 리스트
    /// @param workplaceId 소속되어야 할 Workplace ID
    /// @return 조건에 맞는 Worker 리스트
    @Select("""
        <script>
            SELECT * FROM workers
            WHERE workplace_id = #{workplaceId}
            AND id IN
            <foreach item="id" collection="workerIdList" open="(" separator="," close=")">
                #{id}
            </foreach>
        </script>
    """)
    List<Worker> findAllByIdInAndWorkplaceId(
            @Param("workerIdList") List<Long> workerIdList,
            @Param("workplaceId") Long workplaceId
    );

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
    /// `user_id != ?`만 쓰면 NULL 행이 빠진다(NULL과의 비교는 참이 아니다). 과거 하드 삭제로 `user_id`가 NULL이 된
    /// 근무자는 기록이 남아 있는데 사장님 목록에서 사라졌다(스코프 4 탈퇴 신호 #1). "탈퇴한 근무자"로 보여야 한다.
    @Select("SELECT * FROM workers WHERE workplace_id = #{workplaceId} AND (user_id IS NULL OR user_id != #{excludeUserId})")
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
    /// `user_id` 조건을 넣으면 안 된다. 호출자가 넘기는 것은 **사장님의 userId**인데
    /// 대상 행의 `user_id`는 **알바생의 userId**라 절대 매치되지 않고, 서버는 204를 주지만
    /// 실제로는 아무것도 바뀌지 않는다. `id`가 PK이고 `workplace_id`가 테넌시 가드다.
    @Update("UPDATE workers SET owner_based_label_color = #{ownerBasedLabelColor} WHERE id = #{id} AND workplace_id = #{workplaceId}")
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

    /// 근무자 행을 지운다. `user_id`를 조건에 넣지 않는다 — 과거 하드 삭제로 `user_id`가 NULL인 행은
    /// `user_id = NULL`이 항상 거짓이라 지워지지 않았다(스코프 4 탈퇴 신호 #5).
    @Delete("DELETE FROM workers WHERE id = #{id} AND workplace_id = #{workplaceId}")
    void deleteByIdAndWorkplaceId(@Param("id") Long id, @Param("workplaceId") Long workplaceId);

    /// 사장님이 근무 관계를 끝낸다. **행은 지우지 않는다** — 지우면 CASCADE로 그 알바생의 근무·급여 기록이
    /// 전부 사라진다(스코프 4 I4). 이미 끝난 관계면 아무것도 바꾸지 않는다.
    @Update("""
            UPDATE workers SET left_at = CURRENT_TIMESTAMP(), is_now_working = 0
            WHERE id = #{id} AND workplace_id = #{workplaceId} AND left_at IS NULL
            """)
    void updateLeftAt(@Param("id") Long id, @Param("workplaceId") Long workplaceId);

    /// 끝난 근무 관계를 다시 연다(재참여). 승인은 처음부터 다시 받는다.
    @Update("""
            UPDATE workers SET left_at = NULL, is_accepted = 0, worker_based_label_color = #{workerBasedLabelColor}
            WHERE id = #{id} AND left_at IS NOT NULL
            """)
    void reactivate(@Param("id") Long id, @Param("workerBasedLabelColor") String workerBasedLabelColor);
}
