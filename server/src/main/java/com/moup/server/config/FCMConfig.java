package com.moup.server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@RequiredArgsConstructor
public class FCMConfig {

  private final DefaultResourceLoader resourceLoader;
  @Value("${firebase.account-key.path}")
  private String fireBaseKeyPath;

  @Bean
  public FirebaseApp initializeFirebase() throws IOException {

    Resource resource = resourceLoader.getResource(fireBaseKeyPath);

    try (InputStream serviceAccount = resource.getInputStream()) {
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .build();
      if (FirebaseApp.getApps().isEmpty()) { // 앱이 이미 초기화되지 않았는지 확인
        return FirebaseApp.initializeApp(options);
      } else {
        return FirebaseApp.getInstance(); // 이미 초기화된 경우 기존 인스턴스 반환
      }
    }
  }
}
