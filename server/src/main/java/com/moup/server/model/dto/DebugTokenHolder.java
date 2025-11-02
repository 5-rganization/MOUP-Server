package com.moup.server.model.dto;

import lombok.Getter;
import lombok.Setter;

/// 개발/테스트 환경 전용 디버그 토큰 보관용 Bean
/// `@Component`가 없는 순수 자바 클래스(POJO)입니다.
/// `@Configuration` 파일에서 @Bean으로 직접 생성됩니다.
@Getter
@Setter
public class DebugTokenHolder {
    // 토큰 생성 전 기본값
    private String adminToken = "Token generating...";
    private String ownerToken = "Token generating...";
    private String workerToken = "Token generating...";
}