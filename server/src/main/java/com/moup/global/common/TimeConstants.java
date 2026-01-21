package com.moup.global.common;

import java.time.ZoneId;

public final class TimeConstants {
    /// 대한민국 서울의 ZoneId (Asia/Seoul)
    public static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    /// 이 클래스는 인스턴스화할 수 없습니다.
    private TimeConstants() {}
}
