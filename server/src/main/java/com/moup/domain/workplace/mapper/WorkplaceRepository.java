package com.moup.domain.workplace.mapper;

import com.moup.domain.workplace.domain.Workplace;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorkplaceRepository {

    /// 근무지를 생성하는 메서드
    ///
    /// @param workplace 생성할 근무지 Workplace 객체
    /// @return 생성된 행의 수
    @Insert("""
            INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude)
            VALUES (#{ownerId}, #{workplaceName}, #{categoryName}, #{isShared}, #{address}, #{latitude}, #{longitude})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Workplace workplace);

    /// 근무지 ID를 통해 해당 근무지가 존재하는지 여부를 반환하는 메서드
    ///
    /// @param id 조회할 근무지 ID
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM workplaces WHERE id = #{id})")
    boolean existsById(Long id);

    /// 등록자 ID, 근무지 이름을 통해 해당 근무지가 존재하는지(등록자가 이미 만들었는지) 여부를 반환하는 메서드
    ///
    /// @param ownerId       조회할 등록자 ID
    /// @param workplaceName 조회할 근무지 이름
    /// @return 존재하면 true, 그렇지 않으면 false
    @Select("SELECT EXISTS(SELECT 1 FROM workplaces WHERE owner_id = #{ownerId} AND workplace_name = #{workplaceName})")
    boolean existsByOwnerIdAndWorkplaceName(Long ownerId, String workplaceName);

    /// 근무지 ID를 통해 해당 근무지를 찾고, 그 근무지의 객체를 반환하는 메서드
    ///
    /// @param id 조회할 근무지의 ID
    /// @return 조회된 Workplace 객체, 없으면 Optional.empty
    @Select("SELECT * FROM workplaces WHERE id = #{id}")
    Optional<Workplace> findById(Long id);

    /// 여러 근무지 ID에 해당하는 모든 근무지를 조회하는 메서드
    ///
    /// @param idList 조회할 근무지 ID 리스트
    /// @return 조회된 Workplace 객체 리스트
    @Select("""
            <script>
                SELECT * FROM workplaces
                WHERE id IN
                <foreach item="id" collection="idList" open="(" separator="," close=")">
                    #{id}
                </foreach>
            </script>
            """)
    List<Workplace> findAllByIdListIn(@Param("idList") List<Long> idList);

    /// 등록자 ID에 해당하는 모든 매장을 조회하는 메서드
    ///
    /// @param ownerId 조회할 등록자 ID
    /// @return 조회된 Workplace 객체 리스트
    @Select("SELECT * FROM workplaces WHERE owner_id = #{ownerId}")
    List<Workplace> findAllByOwnerId(@Param("ownerId") Long ownerId);

    /// 근무지의 ID와 등록자 ID에 해당하는 근무지를 업데이트하는 메서드
    ///
    /// @param workplace 업데이트할 Workplace 객체
    /// PATCH 의미대로 **전달된 필드만** 갱신한다. 예전에는 모든 컬럼을 무조건 덮어써서,
    /// 클라이언트가 주소·좌표를 빠뜨리면 저장돼 있던 값이 NULL로 지워졌다.
    ///
    /// `address`/`latitude`/`longitude`는 스키마상 NULL 허용이라 "값이 없는 근무지"가
    /// 정당한 상태다. 그래서 필수로 만들어 막을 수도 없었다.
    ///
    /// ponytail: null = "건드리지 않음"이라 주소를 명시적으로 **지울** 수단이 없다.
    /// 지금 주소 삭제 기능이 없어 무해하다. 필요해지면 빈 문자열을 삭제로 약속하면 된다.
    @Update("""
            <script>
            UPDATE workplaces
            <set>
                <if test="workplaceName != null">workplace_name = #{workplaceName},</if>
                <if test="categoryName != null">category_name = #{categoryName},</if>
                <if test="address != null">address = #{address},</if>
                <if test="latitude != null">latitude = #{latitude},</if>
                <if test="longitude != null">longitude = #{longitude},</if>
            </set>
            WHERE id = #{id} AND owner_id = #{ownerId}
            </script>
            """)
    void update(Workplace workplace);

    /// 근무지 ID와 등록자 ID에 해당하는 근무지를 삭제하는 메서드.
    ///
    /// @param id      삭제할 근무지의 ID
    /// @param ownerId 삭제할 근무지의 등록자 ID
    @Delete("DELETE FROM workplaces WHERE id = #{id} AND owner_id = #{ownerId}")
    void delete(Long id, Long ownerId);

    @Select("SELECT owner_id FROM workplaces WHERE id = #{workplaceId}")
    Long findOwnerId(Long workplaceId);

    @Select("SELECT COUNT(*) FROM workplaces WHERE owner_id = #{userId}")
    int getOwnedWorkplaceCountByUserId(Long userId);
}
