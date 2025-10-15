package com.moup.server.repository;

import com.moup.server.model.dto.AdminAlarmRequest;
import com.moup.server.model.dto.AnnouncementRequest;
import com.moup.server.model.dto.NormalAlarmRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface AlarmRepository {

    @Insert("INSERT INTO normal_alarms (sender_id, receiver_id, title, content) VALUES (#{senderId}, #{receiverId}, #{title}, #{content})")
    public void saveNormalAlarm(NormalAlarmRequest request);

    @Insert("INSERT INTO admin_alarms (title, content) VALUES (#{title}, #{content})")
    public void saveAdminAlarm(AdminAlarmRequest request);


}
