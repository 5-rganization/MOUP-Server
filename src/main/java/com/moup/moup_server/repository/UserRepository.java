package com.moup.moup_server.repository;

import com.moup.moup_server.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserRepository {

    /**
     * DB에 유저 정보 저장
     *
     * @param user
     * @return result
     */
    @Insert("INSERT INTO users (id, provider, provider_id, role, created_at) VALUES (#{id}, #{provider}, #{providerId}, #{role}, #{createdAt})")
    int createUser(User user);

    /**
     * id를 통해 DB에서 유저 가져오기
     *
     * @param id
     * @return User
     */
    @Select("SELECT * FROM USERS WHERE id = #{id}")
    Optional<User> findById(Long id);
}
