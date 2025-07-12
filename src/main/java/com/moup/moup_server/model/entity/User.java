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
    private @NonNull Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private Login provider;
    private @NonNull String providerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private @NonNull Role role;
    private @NonNull String createdAt;
}
