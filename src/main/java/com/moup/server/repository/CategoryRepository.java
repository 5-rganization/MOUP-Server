package com.moup.server.repository;

import com.moup.server.model.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

public interface CategoryRepository {
    /**
     * 근무지 카테고리를 생성하고, 생성된 카테고리의 ID를 반환하는 메서드.
     *
     * @param categoryName 생성할 카테고리의 이름
     * @return 생성된 카테고리의 ID
     */
    @Insert("INSERT INTO workplace_categories (category_name) VALUES (#{categoryName})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer create(String categoryName);

    /**
     * 카테고리 이름을 통해 해당 카테고리를 찾고, 그 카테고리의 객체를 반환하는 메서드.
     *
     * @param categoryName 조회할 카테고리의 이름
     * @return 조회된 Category 객체, 없으면 Optional.empty
     */
    @Select("SELECT * FROM workplace_categories WHERE category_name = #{categoryName}")
    Optional<Category> findByCategoryName(String categoryName);

    /**
     * id에 해당하는 카테고리의 이름을 업데이트하는 메서드.
     *
     * @param id 업데이트할 카테고리의 ID
     * @param newCategoryName 업데이트할 카테고리의 새 이름
     */
    @Update("UPDATE workplace_categories SET category_name = #{newCategoryName} WHERE id = #{id}")
    void update(Integer id, String newCategoryName);

    /**
     * id에 해당하는 카테고리를 삭제하는 메서드.
     *
     * @param id 삭제할 카테고리의 ID
     */
    @Delete("DELETE FROM workplace_categories WHERE id = #{id}")
    void delete(Integer id);
}
