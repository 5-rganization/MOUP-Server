package com.moup.server.model.entity;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
public class User {
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private Login provider;
    private String providerId;
    private String username;
    private String nickname;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;
    private String profileImg;
    private String createdAt;
}
