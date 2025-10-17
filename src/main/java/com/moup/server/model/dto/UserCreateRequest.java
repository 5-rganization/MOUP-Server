package com.moup.server.model.dto;

import com.moup.server.common.Login;
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
