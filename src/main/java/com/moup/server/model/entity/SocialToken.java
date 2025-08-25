package com.moup.server.model.entity;

import com.moup.server.common.Login;
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
    // TODO: Provider 넣을지? Apple만 필요하기 때문에
    private String refreshToken;
    private LocalDateTime updatedAt;
}
