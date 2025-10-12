package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SocialToken {
    private Long id;
    private Long userId;
    private String refreshToken;
    private LocalDateTime updatedAt;
}
