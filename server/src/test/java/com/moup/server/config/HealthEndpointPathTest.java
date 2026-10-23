package com.moup.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/// 액추에이터 헬스 엔드포인트가 **정말 `/health`에 뜨는지** 확인한다.
///
/// `base-path=/`를 쓰면 경로가 `//health`가 될 수 있다. 그러면 외부 모니터가
/// 404를 받고 서버가 죽은 줄 알거나, 반대로 죽었는데 모른다. 앱 전체를 띄우려면
/// DB·Redis가 필요하므로, 스프링이 실제로 쓰는 `EndpointMapping`에
/// application.properties의 값을 그대로 먹여 경로만 계산해 본다.
///
/// SecurityConfig의 `HEALTH_CHECK_URL = {"/health"}`도 이 경로에 맞춰져 있다.
/// 둘 중 하나만 바뀌면 헬스체크가 401을 받는다.
public class HealthEndpointPathTest {

  private static Properties applicationProperties() throws Exception {
    Properties properties = new Properties();
    try (InputStream in = HealthEndpointPathTest.class
        .getResourceAsStream("/application.properties")) {
      assertNotNull(in, "application.properties를 클래스패스에서 찾지 못했다");
      properties.load(in);
    }
    return properties;
  }

  @Test
  @DisplayName("설정된 base-path로 헬스 엔드포인트가 /health에 매핑된다")
  void 헬스_경로가_유지된다() throws Exception {
    Properties properties = applicationProperties();
    String basePath = properties.getProperty("management.endpoints.web.base-path");

    assertEquals("/", basePath, "base-path가 바뀌면 /health가 사라진다");
    assertEquals("/health", new EndpointMapping(basePath).createSubPath("health"),
        "액추에이터가 계산하는 실제 경로");
  }

  @Test
  @DisplayName("health 외의 액추에이터 엔드포인트는 노출되지 않는다")
  void 헬스만_노출된다() throws Exception {
    // base-path가 /라서 노출 목록이 넓어지면 /env·/beans 같은 것이 루트에 뜬다.
    assertEquals("health",
        applicationProperties().getProperty("management.endpoints.web.exposure.include"));
  }

  @Test
  @DisplayName("미인증 요청에는 컴포넌트 상세를 주지 않는다")
  void 상세는_인증된_요청에만() throws Exception {
    // /health는 permitAll이다. 상세를 열면 어느 컴포넌트가 죽었는지가 정찰 정보가 된다.
    assertEquals("when-authorized",
        applicationProperties().getProperty("management.endpoint.health.show-details"));
  }
}
