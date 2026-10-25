package com.moup.domain.auth.exception;

import jakarta.security.auth.message.AuthException;

/// 다시 시도해도 성공할 수 없는 소셜 연동 해제 실패.
///
/// - 해제에 쓸 크리덴셜(refresh token)이 서버에 없다 — Google은 이미 동의한 계정에 refresh token을 다시 주지 않아,
///   예전 코드 시절 해제 없이 지워졌다가 재가입한 계정이 이렇게 된다(스코프 1 확정 요건 1-A).
/// - 공급자가 토큰이 이미 무효라고 답했다(`invalid_token`·`invalid_grant`) — 사용자가 계정 설정에서 직접 끊었거나 만료됐다.
///
/// 예전에는 이것도 일시 실패로 보고 약 27일 동안 매일 다시 시도하며 ERROR 로그를 남긴 뒤에야 포기했다(1-B).
public class PermanentRevokeFailureException extends AuthException {
    public PermanentRevokeFailureException(String message) {
        super(message);
    }
}
