package com.moup.global.config;

import com.moup.global.util.StringToViewTypeConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToViewTypeConverter());
  }

  // CORS는 SecurityConfig 한 곳에서만 정의한다.
  // 여기에도 있으면 두 설정이 조용히 어긋나고(실제로 PATCH가 한쪽에만 있었다),
  // 시큐리티 필터 체인 쪽이 이기므로 이 설정은 착각만 준다.
}
