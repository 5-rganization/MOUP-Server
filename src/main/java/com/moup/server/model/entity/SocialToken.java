package com.moup.server.model.entity;

import com.moup.server.common.Login;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SocialToken {
    private Long id;
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private String updatedAt;
}
