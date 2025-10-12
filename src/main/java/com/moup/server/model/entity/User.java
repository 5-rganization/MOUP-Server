package com.moup.server.model.entity;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private Login provider;
    private String providerId;
    private String username;
    private String nickname;
    private Role role;
    private String profileImg;
    private String createdAt;
    private String deletedAt;
    private boolean isDeleted;
    private String fcmToken;
}
