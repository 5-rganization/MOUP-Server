package com.moup.server.repository;

import com.moup.server.model.entity.Worker;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkerRepository {

    /**
     * 근무자를 생성하는 메서드.
     *
     * @param worker 생성할 근무자 엔티티
     * @return 생성된 행의 수
     */
    @Insert("INSERT INTO workers (user_id, workplace_id, label_color, is_accepted) VALUES (#{userId}, #{workplaceId}, #{labelColor}, #{isAccepted})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Worker worker);

    /**
     * 근무지 ID와 유저 ID를 통해 해당 근무지 유저의 근무자 객체를 반환하는 메서드
     *
     * @param userId 조회할 유저 ID
     * @param workplaceId 조회할 근무지 ID
     * @return 조회된 Worker 객체, 없으면 Optional.empty
     */
    @Select("SELECT * FROM workers WHERE user_id = #{userId} AND workplace_id = #{workplaceId}")
    Optional<Worker> findByUserIdAndWorkplaceId(Long userId, Long workplaceId);

    /**
     * 유저 ID를 통해 해당 유저의 모든 근무자 객체를 리스트로 반환하는 메서드
     *
     * @param userId 조회할 유저 ID
     * @return 조회된 Worker 객체 리스트, 없으면 빈 배열
     */
    @Select("SELECT * FROM workers WHERE user_id = #{userId}")
    List<Worker> findAllByUserId(Long userId);

    /**
     * ID에 해당하는 근무자의 라벨 색상을 업데이트하는 메서드.
     *
     * @param id 업데이트할 근무자의 ID
     * @param workplaceId 업데이트할 근무자의 근무지 ID
     * @param labelColor 업데이트할 라벨 색상
     */
    @Update("UPDATE workers SET label_color = #{labelColor} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateLabelColor(Long id, Long userId, Long workplaceId, String labelColor);

    /**
     * ID에 해당하는 근무자의 초대 승인 여부를 업데이트하는 메서드.
     *
     * @param id 업데이트할 근무자의 ID
     * @param userId 업데이트할 근무자의 유저 ID
     * @param workplaceId 업데이트할 근무자의 근무지 ID
     * @param isAccepted 업데이트할 초대 승인 여부
     */
    @Update("UPDATE workers SET is_accepted = #{isAccepted} WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void updateIsAccepted(Long id, Long userId, Long workplaceId, boolean isAccepted);

    /**
     * 근무자 ID와 근무지 ID에 해당하는 근무자를 삭제하는 메서드.
     *
     * @param id 삭제할 근무자의 ID
     * @param userId 삭제할 근무자의 유저 ID
     * @param workplaceId 삭제할 근무자의 근무지 ID
     */
    @Delete("DELETE FROM workers WHERE id = #{id} AND user_id = #{userId} AND workplace_id = #{workplaceId}")
    void delete(Long id, Long userId, Long workplaceId);
}
