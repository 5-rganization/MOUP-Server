package com.moup.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/// 서버 내부 스케줄러를 켠다.
///
/// 탈퇴 유예기간이 지난 사용자의 하드 삭제는 원래 라즈베리파이의 cron이
/// `delete_old_users.sh`를 실행해 `/admin/users`를 호출하는 구조였다.
/// 그런데 그 스크립트는 cron이 가리키는 경로에 존재하지 않았고
/// (`delete_old_users.log`에 No such file or directory만 쌓여 있었다),
/// 등록 로직마저 deploy.yml에서 통째로 주석 처리돼 있었다.
/// 즉 **배치가 한 번도 돈 적이 없고**, 탈퇴한 사용자의 데이터가 계속 남아 있었다.
///
/// 그 구조는 스크립트 위치 · crontab 등록 · ADMIN_AUTH_TOKEN 유효성 ·
/// 서버 URL 네 가지가 모두 일치해야 동작하는데, 어느 하나가 어긋나도
/// 조용히 멈추고 아무도 모른다. 실제로 그렇게 됐다.
///
/// 앱 안으로 옮기면 넷 다 필요 없다. 서버가 뜨면 배치도 돈다.
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
