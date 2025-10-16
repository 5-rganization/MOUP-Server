package com.moup.server.repository;

import com.moup.server.model.dto.AdminAlarmRequest;
import com.moup.server.model.dto.NormalAlarmRequest;
import com.moup.server.model.entity.AdminAlarm;
import com.moup.server.model.entity.Announcement;
import com.moup.server.model.entity.NormalAlarm;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.swing.text.html.Option;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmRepository {

  @Insert("INSERT INTO normal_alarms (sender_id, receiver_id, title, content) VALUES (#{senderId}, #{receiverId}, #{title}, #{content})")
  void saveNormalAlarm(NormalAlarmRequest request);

  @Insert("INSERT INTO admin_alarms (title, content) VALUES (#{title}, #{content})")
  void saveAdminAlarm(AdminAlarmRequest request);

  @Select("SELECT * FROM admin_alarms")
  List<AdminAlarm> findAllAdminAlarms();

  @Select("SELECT * FROM normal_alarms WHERE receiver_id = #{userId}")
  List<NormalAlarm> findAllNormalAlarmsByUserId(Long userId);

  @Select("SELECT * FROM normal_alarms WHERE id = #{notificationId} AND receiver_id = #{userId}")
  Optional<NormalAlarm> findNormalAlarmById(Long userId, Long notificationId);

  @Update("UPDATE normal_alarms SET read_at = #{readTime} WHERE id = #{notificationId} AND receiver_id = #{userId}")
  void updateReadAtById(Long userId, Long notificationId, LocalDateTime readTime);

  @Delete("DELETE FROM normal_alarms WHERE id = #{notificationId}")
  void deleteNormalAlarmById(Long notificationId);
}
