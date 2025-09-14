package com.moup.server.repository;

import com.moup.server.model.entity.Workplace;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

@Mapper
public interface WorkplaceRepository {

    /**
     * 근무지를 생성하고, 생성된 근무지의 ID를 반환하는 메서드.
     *
     * @param workplace 생성할 근무지 엔티티
     * @return 생성된 근무지의 ID
     */
    @Insert("""
            INSERT INTO workplaces (owner_id, workplace_name, category_name, label_color, is_shared, address, latitude, longitude) 
            VALUES (#{ownerId}, #{workplaceName}, #{categoryName}, #{labelColor}, #{isShared}, #{address}, #{latitude}, #{longitude})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(Workplace workplace);

    /**
     * 근무지 ID를 통해 해당 근무지를 찾고, 그 근무지의 객체를 반환하는 메서드
     *
     * @param id 조회할 근무지의 ID
     * @return 조회된 Workplace 객체, 없으면 Optional.empty
     */
    @Select("SELECT * FROM workplaces WHERE id = #{id}")
    Optional<Workplace> findById(Long id);

    /**
     * ID에 해당하는 근무지를 업데이트하는 메서드.
     *
     * @param id 업데이트할 근무지의 ID
     * @param workplace 업데이트할 근무지 엔티티
     */
    @Update("""
            UPDATE workplaces
            SET workplace_name = #{workplaceName}, category_name = #{categoryName}, label_color = #{labelColor}, is_shared = #{isShared}, address = #{address}, latitude = #{latitude}, longitude = #{longitude}
            WHERE id = #{id}
            """)
    void updateById(Long id, Workplace workplace);

    /**
     * ID에 해당하는 근무지를 삭제하는 메서드.
     *
     * @param id 삭제할 근무지의 ID
     */
    @Delete("DELETE FROM workplaces WHERE id = #{id}")
    void deleteById(Long id);
}
