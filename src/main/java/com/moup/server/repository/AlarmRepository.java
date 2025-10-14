package com.moup.server.repository;

import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmRepository {

    @Insert("")
    public void save();
}
