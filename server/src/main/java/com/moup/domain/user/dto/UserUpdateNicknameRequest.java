package com.moup.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
@Schema(description = "유저 닉네임 변경을 위한 DTO")
public class UserUpdateNicknameRequest {
    // users.nickname VARCHAR(20). 넘기면 422가 아니라 DB 오류로 500이 났다.
    @NotBlank(message = "필수 입력값입니다.")
    @Size(max = 20, message = "20자 이내로 입력해야 합니다.")
    @Schema(description = "변경할 닉네임", example = "moup1234")
    private String nickname;
}
