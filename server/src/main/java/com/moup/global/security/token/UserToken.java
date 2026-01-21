package com.moup.global.security.token;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class UserToken {
    private Long id;
    private Long userId;
    private String refreshToken;
    private LocalDateTime expiryDate;
    private String createdAt;
}
