package com.moup.domain.user.domain;

import com.moup.domain.auth.domain.Login;
import com.moup.global.common.type.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Login provider;

    @Column(length = 100, nullable = false)
    private String providerId;

    @Column(length = 20)
    private String username;

    @Column(length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String profileImg;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(columnDefinition = "TEXT")
    private String fcmToken;

    @Builder
    public User(Login provider, String providerId, String username, String nickname, Role role, String profileImg, String fcmToken) {
        this.provider = provider;
        this.providerId = providerId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.profileImg = profileImg;
        this.fcmToken = fcmToken;
        this.isDeleted = false;
    }

    public void withdraw() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
