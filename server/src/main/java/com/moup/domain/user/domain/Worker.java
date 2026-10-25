package com.moup.domain.user.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class Worker {
    private Long id;
    private Long userId;
    private Long workplaceId;
    private String workerBasedLabelColor;
    private String ownerBasedLabelColor;
    private Boolean isAccepted;
    private Boolean isNowWorking;
    /// 사장님이 근무 관계를 끝낸 시각. NULL이면 재직 중이다 (Phase 11 D11-5).
    private LocalDateTime leftAt;

    /// 근무가 끝난 근무지는 기록 조회만 된다.
    public boolean hasLeft() {
        return leftAt != null;
    }
}
