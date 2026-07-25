package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.domain.NormalAlarm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NormalAlarmRepository extends JpaRepository<NormalAlarm, Long> {

  List<NormalAlarm> findAllByReceiverId(Long userId);

  Optional<NormalAlarm> findByIdAndReceiverId(Long id, Long userId);

  void deleteAllByReceiverId(Long userId);

  @Modifying(clearAutomatically = true) // UPDATE나 DELETE 쿼리에는 필수! (DB와 메모리 싱크를 맞춤)
  @Query("UPDATE NormalAlarm n SET n.readAt = CURRENT_TIMESTAMP WHERE n.receiver.id = :userId AND n.readAt IS NULL")
  void markAllAsReadByUserId(@Param("userId") Long userId);
}
