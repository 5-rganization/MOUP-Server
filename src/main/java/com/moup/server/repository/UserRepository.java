package com.moup.server.repository;

import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserRepository {

    /**
     * DB에 유저 정보 저장
     */
    @Insert("INSERT INTO users (provider, provider_id, username, nickname, role) VALUES (#{provider}, #{providerId}, #{username}, #{nickname}, #{role})")
    void createUser(RegisterRequest registerRequest);

    /**
     * id를 통해 DB에서 유저 가져오기
     *
     * @param id
     * @return User
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    /**
     * 소셜 로그인 id를 통해 DB에서 유저 검색하기
     * 
     * @param providerId
     * @return User
     */
    @Select("SELECT * FROM users WHERE provider_id = #{providerId}")
    Optional<User> findByProviderId(String providerId);

    /**
     * 유저 이름을 통해 DB에서 유저 검색하기
     *
     * @param username
     * @return User
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);
}
