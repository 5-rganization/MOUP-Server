package com.moup.domain.user.domain;

import com.moup.domain.auth.domain.Login;
import com.moup.global.common.type.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    @Enumerated(EnumType.STRING)
    private Login provider;
    private String providerId;
    private String username;
    private String nickname;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String profileImg;
    private String createdAt;
    private String deletedAt;
    private boolean isDeleted;
    /// 가명처리(탈퇴 확정)가 끝난 시각. NULL이면 아직 처리 전이다.
    /// `isDeleted`만으로는 "탈퇴 신청됨"과 "탈퇴 처리 완료"를 구분할 수 없다.
    private String anonymizedAt;
}
