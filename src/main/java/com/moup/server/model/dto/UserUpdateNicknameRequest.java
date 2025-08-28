package com.moup.server.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "유저 닉네임 변경을 위한 DTO")
public class UserUpdateNicknameRequest {
    @Schema(description = "변경할 닉네임", example = "moup1234")
    private String nickname;
}
