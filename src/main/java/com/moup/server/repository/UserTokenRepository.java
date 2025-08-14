package com.moup.server.repository;


import com.moup.server.model.entity.UserToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
public interface UserTokenRepository {

    @Select("SELECT * FROM user_tokens WHERE user_id = #{userId}")
    Optional<UserToken> findByUserId(Long userId);
}
