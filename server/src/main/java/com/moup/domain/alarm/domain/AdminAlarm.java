package com.moup.domain.alarm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.moup.global.common.domain.TimeConstants.SEOUL_ZONE_ID;

@Entity
@Getter
@Table(name = "admin_alarms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAlarm {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false, updatable = false)
  private LocalDateTime sentAt;

  @Builder
  public AdminAlarm(String title, String content) {
    this.title = title;
    this.content = content;
  }

  @PrePersist
  void initializeSentAt() {
    if (sentAt == null) {
      sentAt = LocalDateTime.now(SEOUL_ZONE_ID);
    }
  }
}
