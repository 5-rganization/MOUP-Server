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

import com.moup.server.model.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AlarmRepository {

    @Insert("INSERT INTO normal_alarms (sender_id, receiver_id, title, content) VALUES (#{senderId}, #{receiverId}, #{title}, #{content})")
    void saveNormalAlarm(NormalAlarmRequest request);

    @Insert("INSERT INTO admin_alarms (title, content) VALUES (#{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void saveAdminAlarm(Announcement announcement);

    @Select("SELECT * FROM normal_alarms WHERE receiver_id = #{userId}")
    List<NormalAlarm> findAllNormalAlarmsByUserId(Long userId);

    @Select("SELECT * FROM normal_alarms WHERE id = #{notificationId} AND receiver_id = #{userId}")
    Optional<NormalAlarm> findNormalAlarmById(Long userId, Long notificationId);

    @Update("UPDATE normal_alarms SET read_at = #{readTime} WHERE id = #{notificationId} AND receiver_id = #{userId}")
    void updateReadAtById(Long userId, Long notificationId, LocalDateTime readTime);

    @Update("UPDATE normal_alarms SET read_at = CURRENT_TIMESTAMP() WHERE receiver_id = #{userId} AND read_at IS NULL")
    void updateAllReadAtByUserId(Long userId);

    @Delete("DELETE FROM normal_alarms WHERE id = #{notificationId}")
    void deleteNormalAlarmById(Long notificationId);

    @Delete("DELETE FROM normal_alarms WHERE receiver_id = #{userId}")
    void deleteAllNormalAlarmByUserId(Long userId);

    @Insert("INSERT INTO admin_alarms (title, content) VALUES (#{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void saveAnnouncement(Announcement announcement);

    void saveAnnouncementMappingForAllUsers(@Param("announcementId") Long announcementId, @Param("users") List<User> users);

    @Select("SELECT * FROM admin_alarms JOIN admin_alarm_user_mappings ON admin_alarms.id = admin_alarm_user_mappings.alarm_id WHERE user_id = #{userId} AND alarm_id = #{announcementId} AND deleted_at IS NULL")
    Optional<AdminAlarm> findAdminAlarmById(Long userId, Long announcementId);

    @Select("SELECT * FROM admin_alarms JOIN admin_alarm_user_mappings ON admin_alarms.id = admin_alarm_user_mappings.alarm_id WHERE user_id = #{userId} AND deleted_at IS NULL")
    List<AdminAlarm> findAllAdminAlarmsByUserId(Long userId);

    @Update("UPDATE admin_alarm_user_mappings SET read_at = CURRENT_TIMESTAMP() WHERE user_id = #{userId} AND alarm_id = #{announcementId} AND read_at IS NULL AND deleted_at IS NULL")
    void updateAnnouncementReadAtById(Long userId, Long announcementId);

    @Update("UPDATE admin_alarm_user_mappings SET read_at = CURRENT_TIMESTAMP() WHERE user_id = #{userId} AND read_at IS NULL AND deleted_at IS NULL")
    void updateAllAnnouncementReadAtByUserId(Long userId);

    @Update("UPDATE admin_alarm_user_mappings SET deleted_at = CURRENT_TIMESTAMP() WHERE user_id = #{userId} AND alarm_id = #{announcementId} AND deleted_at IS NULL")
    void updateAnnouncementDeletedAtById(Long userId, Long announcementId);

    @Update("UPDATE admin_alarm_user_mappings SET deleted_at = CURRENT_TIMESTAMP() WHERE user_id = #{userId} AND deleted_at IS NULL")
    void updateAllAnnouncementDeletedAtByUserId(Long userId);
}
