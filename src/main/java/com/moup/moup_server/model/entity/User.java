package com.moup.moup_server.model.entity;

import com.moup.moup_server.common.Login;
import com.moup.moup_server.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@NoArgsConstructor
public class User {
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private Login provider;
    private String providerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;
    private String createdAt;
}
