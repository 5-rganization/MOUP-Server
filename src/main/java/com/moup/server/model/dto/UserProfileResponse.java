package com.moup.server.model.dto;

import com.moup.server.common.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String username;
    private String nickname;
    private String profileImg;
    private Role role;
    private String createdAt;
}
