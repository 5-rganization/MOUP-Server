package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 닉네임 변경 응답 DTO")
public class UserUpdateNicknameResponse {
    @Schema(description = "유저 ID", example = "1")
    Long userId;
    @Schema(description = "변경된 닉네임", example = "moup1234")
    String nickname;
}
