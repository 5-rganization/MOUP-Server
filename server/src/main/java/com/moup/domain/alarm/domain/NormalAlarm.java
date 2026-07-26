package com.moup.domain.alarm.domain;

import com.moup.domain.user.domain.User;
import com.moup.global.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "normal_alarms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NormalAlarm extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id")
  private User receiver;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  private LocalDateTime sentAt;

  private LocalDateTime readAt;

  @Builder
  public NormalAlarm(User sender, User receiver, String title, String content) {
    this.sender = sender;
    this.receiver = receiver;
    this.title = title;
    this.content = content;
  }

  @PrePersist
  void initializeSentAt() {
    if (sentAt == null) {
      sentAt = LocalDateTime.now(SEOUL_ZONE_ID);
    }
  }

  public void read() {
    this.readAt = LocalDateTime.now(SEOUL_ZONE_ID);
  }
}
