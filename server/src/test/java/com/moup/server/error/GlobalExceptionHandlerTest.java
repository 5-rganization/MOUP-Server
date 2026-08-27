package com.moup.server.error;

import com.moup.global.common.response.ErrorResponse;
import com.moup.global.error.ErrorCode;
import com.moup.global.error.GlobalExceptionHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// 인가 거부가 500이 아니라 403으로 나가는지 검증한다 (스코프 7 C1).
///
/// 수정 전에는 `@ExceptionHandler(RuntimeException.class)` catch-all이
/// `AuthorizationDeniedException`(→ `AccessDeniedException` → `RuntimeException`)을 먼저
/// 잡아 500을 반환하고 정상 종료했다. 그래서 `SecurityConfig`에 등록한
/// `accessDeniedHandler`가 있는 `ExceptionTranslationFilter`는 예외를 보지도 못했다.
///
/// `@PreAuthorize`가 걸린 18개 엔드포인트의 인가 거부가 전부 서버 오류로 위장되고 있었고,
/// 각 컨트롤러의 `@ApiResponse(responseCode = "403")` 문서와도 어긋났다.
public class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("[C1] @PreAuthorize 인가 거부가 403으로 나간다")
  void 인가_거부는_403() {
    // Spring Security 6의 메서드 보안이 실제로 던지는 타입
    AuthorizationDeniedException e =
        new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false));

    ResponseEntity<?> response = handler.handleAccessDenied(e);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "500이 아니라 403이어야 한다");
    assertEquals(ErrorCode.INVALID_PERMISSION_ACCESS.getCode(),
        ((ErrorResponse) response.getBody()).getErrorCode());
  }

  @Test
  @DisplayName("[C1] 일반 AccessDeniedException도 403으로 나간다")
  void 일반_접근거부도_403() {
    ResponseEntity<?> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  @DisplayName("그 외 RuntimeException은 여전히 500이다")
  void 일반_런타임예외는_500() {
    ResponseEntity<?> response = handler.handleException(new IllegalStateException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ================= Phase 8 =================

  @Test
  @DisplayName("[I3(b)] 소셜 인증 실패가 500이 아니라 401로 나간다")
  void 소셜_인증_실패는_401() {
    // AuthException은 checked라 catch-all(RuntimeException)에 걸리지 않았고,
    // 핸들러도 없어 스프링 부트 기본 /error 응답으로 500이 나갔다.
    ResponseEntity<?> response = handler.handleAuthException(
        new jakarta.security.auth.message.AuthException("invalid authorization code"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  @DisplayName("[I6] 그 외 checked 예외도 ErrorResponse 형태로 나간다")
  void checked_예외도_ErrorResponse() {
    ResponseEntity<?> response = handler.handleCheckedException(new Exception("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    // 형태가 중요하다. 부트 기본 /error로 나가면 클라이언트의 에러 파싱이 깨진다.
    assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
        ((ErrorResponse) response.getBody()).getErrorCode());
  }

  @Test
  @DisplayName("[I6] 업로드 용량 초과는 500이 아니라 413")
  void 업로드_용량_초과는_413() {
    ResponseEntity<?> response = handler.handleMaxUploadSize(
        new org.springframework.web.multipart.MaxUploadSizeExceededException(1024L));

    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
  }

  @Test
  @DisplayName("[I16] INVALID_TOKEN은 401이어야 한다")
  void 유효하지_않은_토큰은_401() {
    // 400이면 클라이언트의 "401이면 토큰 갱신 후 재시도" 인터셉터가 동작하지 않아
    // 액세스 토큰이 만료될 때마다 사용자가 그냥 실패를 본다.
    assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN.getHttpStatus());
  }
}
