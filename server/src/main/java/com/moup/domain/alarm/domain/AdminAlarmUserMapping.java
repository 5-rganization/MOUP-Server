package com.moup.domain.alarm.domain;

import com.moup.domain.user.domain.User;
import com.moup.global.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "admin_alarm_user_mappings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAlarmUserMapping extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alarm_id")
  private AdminAlarm adminAlarm;

  private LocalDateTime readAt;

  private LocalDateTime deletedAt;

  @Builder
  public AdminAlarmUserMapping(User user, AdminAlarm adminAlarm) {
    this.user = user;
    this.adminAlarm = adminAlarm;
  }

  public void read() {
    this.readAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
  }

}
