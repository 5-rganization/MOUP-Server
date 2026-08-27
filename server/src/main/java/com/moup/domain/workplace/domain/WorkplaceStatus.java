package com.moup.domain.workplace.domain;

/// 알바생 시점에서 본 근무지 상태.
///
/// 확정 정책 6(승인 대기 가시 범위)과 확정 정책 5(사장님 탈퇴 시 데이터 보존)의
/// 프론트 표시 요구를 열거형 하나로 만족시킨다.
public enum WorkplaceStatus {
    /// 정상. 평소대로 표시한다.
    ACTIVE,
    /// 승인 대기. 목록에는 띄우되 배지로 구분하고 근무 등록 진입을 막는다.
    /// 목록에서 숨기면 대기자가 **자신이 어디에 승인 대기 중인지조차 알 수 없다.**
    PENDING_APPROVAL,
    /// 사장님이 탈퇴했다(`workplaces.owner_id IS NULL`). 데이터는 남지만 접근은 차단된다.
    OWNER_WITHDRAWN
}
