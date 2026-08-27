package com.moup.global.security.token;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserToken {
    private Long id;
    private Long userId;
    private String refreshToken;
    private LocalDateTime expiryDate;
    private String createdAt;
}
