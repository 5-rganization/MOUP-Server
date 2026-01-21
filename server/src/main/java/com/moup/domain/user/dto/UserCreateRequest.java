package com.moup.domain.user.dto;

import com.moup.domain.auth.domain.Login;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreateRequest {
    private Long userId;
    private Login provider;
    private String providerId;
    private String username;
    private String socialRefreshToken;
    private String fcmToken;
}
