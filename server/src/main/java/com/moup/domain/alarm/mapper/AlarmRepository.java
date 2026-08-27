package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.dto.NormalAlarmRequest;
import com.moup.domain.alarm.domain.AdminAlarm;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.domain.NormalAlarm;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.moup.domain.user.domain.User;
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

    /// 탈퇴 확정 시 공지 수신 매핑을 지운다. 소프트 삭제(deleted_at)가 아니라 실제 삭제다 —
    /// 탈퇴한 사용자의 알림함을 남겨둘 이유가 없다.
    /// 하드 삭제를 없앴으므로 users CASCADE가 더 이상 발화하지 않는다.
    @Delete("DELETE FROM admin_alarm_user_mappings WHERE user_id = #{userId}")
    void deleteAllAdminAlarmMappingsByUserId(Long userId);

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
