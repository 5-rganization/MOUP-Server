package com.moup.domain.salary.domain;

import java.time.LocalDate;

/// 급여 설정 변경을 어느 근무부터 적용하는가 (Phase 11 D11-1).
///
/// 근무 행은 등록 시점의 시급·수당 설정을 스냅샷으로 들고 있다(확정 정책 3). 설정을 바꾸면
/// 이 시점 이후 근무의 스냅샷만 새 값으로 갈아 끼우고, 그 이전 근무는 그대로 둔다.
/// 지난 근무일까지 바뀌는 것은 의도가 아니다(스코프 4 Q1 답변).
public enum SalaryApplyFrom {
    /// 오늘 근무부터. 기본값이다.
    TODAY,
    /// 이번 달 1일 근무부터. 앱이 "이번 달 전체에 적용할까요?"를 물어 사용자가 고른 경우.
    THIS_MONTH;

    public LocalDate startDate(LocalDate today) {
        return switch (this) {
            case TODAY -> today;
            case THIS_MONTH -> today.withDayOfMonth(1);
        };
    }
}
