package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.domain.AdminAlarmUserMapping;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AdminAlarmUserMappingRepository extends JpaRepository<AdminAlarmUserMapping, Long> {
  /*
   * 1. JOIN 조회 + Soft Delete 필터링
   * MyBatis: @Select("SELECT * FROM admin_alarms JOIN admin_alarm_user_mappings ON ... WHERE user_id = #{userId} AND deleted_at IS NULL")
   * JPA: JPA에서는 엔티티 안에 이미 연관관계(@ManyToOne)가 걸려 있어서 알아서 JOIN을 해줍니다.
   * 설명: 유저 ID로 매핑 정보를 찾되, '삭제되지 않은(deletedAt IsNull)' 것만 가져와라.
   * 패치 조인(FETCH JOIN): 이 매핑 정보를 가져올 때 연관된 AdminAlarm(공지원본) 정보도 한 번의 쿼리로 같이 가져오라는 뜻(N+1 문제 방지).
   */
  @Query("SELECT m FROM AdminAlarmUserMapping m JOIN FETCH m.adminAlarm WHERE m.user.id = :userId AND m.deletedAt IS NULL")
  List<AdminAlarmUserMapping> findAllActiveByUserId(@Param("userId") Long userId);

  /*
   * 2. 특정 공지사항 단건 조회 (삭제 안 된 것)
   */
  @Query("SELECT m FROM AdminAlarmUserMapping m WHERE m.user.id = :userId AND m.adminAlarm.id = :alarmId AND m.deletedAt IS NULL")
  Optional<AdminAlarmUserMapping> findActiveByUserIdAndAlarmId(@Param("userId") Long userId, @Param("alarmId") Long alarmId);

  /*
   * 3. 대량 삭제 처리 (Soft Delete)
   * MyBatis: @Update("UPDATE admin_alarm_user_mappings SET deleted_at = CURRENT_TIMESTAMP() WHERE user_id = #{userId} AND deleted_at IS NULL")
   */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE AdminAlarmUserMapping m SET m.deletedAt = CURRENT_TIMESTAMP WHERE m.user.id = :userId AND m.deletedAt IS NULL")
  void softDeleteAllByUserId(@Param("userId") Long userId);
}
