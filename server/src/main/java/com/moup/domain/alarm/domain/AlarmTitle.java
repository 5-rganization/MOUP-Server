package com.moup.domain.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlarmTitle {
    ALARM_TITLE_WORKPLACE_JOIN_REQUEST("근무지 참가 요청"),
    ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED("근무지 참가 승인"),
    ALARM_TITLE_WORKPLACE_JOIN_REJECTED("근무지 참가 거부");

    private final String title;
}
