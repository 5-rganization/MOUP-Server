package com.moup.domain.alarm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmContent {
    // 포맷 템플릿으로 정의
    ALARM_CONTENT_WORKPLACE_JOIN_REQUEST("%s님이 근무지 참가 요청을 보냈습니다."), // 요청 근무자 이름
    ALARM_CONTENT_WORKPLACE_JOIN_ACCEPTED("%s 근무지 참가가 승인되었습니다."),   // 승인된 근무지 이름
    ALARM_CONTENT_WORKPLACE_JOIN_REJECTED("%s 근무지 참가가 거부되었습니다."),   // 거부된 근무지 이름
    ALARM_CONTENT_SALARY_UPDATED_BY_OWNER("%s 근무지에서 사장님이 급여 설정을 변경했습니다."),   // 근무지 이름
    ALARM_CONTENT_SALARY_UPDATED_BY_WORKER("%s님이 %s 근무지의 급여 설정을 변경했습니다.");     // 알바생 이름, 근무지 이름

    private final String content;

    public String getContent(Object... args) {
        return String.format(this.content, args);
    }
}
