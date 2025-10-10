package com.moup.server.repository;

import com.moup.server.model.entity.Workplace;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

@Mapper
public interface WorkplaceRepository {

    /// 근무지를 생성하는 메서드.
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
    /// @param ownerId 조회할 등록자 ID
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

    /// 근무지의 ID와 등록자 ID에 해당하는 근무지를 업데이트하는 메서드.
    ///
    /// @param workplace 업데이트할 Workplace 객체
    @Update("""
            UPDATE workplaces
            SET workplace_name = #{workplaceName}, category_name = #{categoryName}, address = #{address}, latitude = #{latitude}, longitude = #{longitude}
            WHERE id = #{id} AND owner_id = #{ownerId}
            """)
    void update(Workplace workplace);

    /// 근무지 ID와 등록자 ID에 해당하는 근무지를 삭제하는 메서드.
    ///
    /// @param id 삭제할 근무지의 ID
    /// @param ownerId 삭제할 근무지의 등록자 ID
    @Delete("DELETE FROM workplaces WHERE id = #{id} AND owner_id = #{ownerId}")
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
