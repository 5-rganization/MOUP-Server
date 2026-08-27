package com.moup.global.security.token;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialToken {
    private Long id;
    private Long userId;
    private String refreshToken;
    private LocalDateTime updatedAt;
}
