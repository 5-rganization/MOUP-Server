package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserToken {
    private Long id;
    private Long userId;
    private String refreshToken;
    private String expiryDate;
    private String createdAt;
}
