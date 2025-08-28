package com.moup.server.model.dto;

import com.moup.server.common.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenCreateRequest {
    Long userId;
    String username;
    Role role;
}
