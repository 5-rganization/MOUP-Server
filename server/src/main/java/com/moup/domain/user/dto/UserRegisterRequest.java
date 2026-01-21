package com.moup.domain.user.dto;

import com.moup.global.common.type.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRegisterRequest {
    private Long userId;
    private String nickname;
    private Role role;
}
