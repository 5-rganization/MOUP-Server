# 스코프 1 — 인증 · 토큰 · 시큐리티

- **범위**: `domain/auth/`, `global/security/`, `JwtUtil`, `AppleJwtUtil`, `SecurityConfig` (~1,450 LOC)
- **판정**: **수정 후** — Critical 2건은 [수정 완료](applied-fixes.md)
- **집계**: Critical 2 (2건 수정 완료) / Important 7 (4건 수정 완료) / Minor 10 / 확인 질문 3 (전부 답변)
- **리뷰 격리**: `docs/review/` 차단, 확정 정책 4건만 전제로 제공
- **특기**: 리뷰어가 라이브러리 동작을 **jar 바이트코드(`javap`)로 직접 검증**했고,
  그 과정에서 **자신의 오탐 1건을 스스로 기각**했다
  (`@PreAuthorize("hasRole('ROLE_ADMIN')")`의 prefix 중복 → `getRoleWithDefaultPrefix`가
  `startsWith("ROLE_")`이면 그대로 반환함을 바이트코드로 확인)

---

## 핵심 진단 — 검증 계층은 견고, 수명주기 계층은 아니다

이 둘을 분리해서 보는 것이 이 서브시스템의 정확한 진단이다.

---

## 토큰 검증 평가 ✅ 깨끗함

| 항목 | Apple | Google | 자체 JWT |
|---|---|---|---|
| **입력 경로** | 클라 → `authCode` → **서버가 공급자와 직접 교환** | 동일 | 서버 발급 |
| 서명 검증 | ✅ `DefaultJWTProcessor` + `JWSVerificationKeySelector(RS256, jwkSet)` (`AppleAuthService:91-105`) | ✅ `GoogleIdTokenVerifier.verify()` → 공개키 순회 검증 | ✅ `verifyWith((SecretKey)key).parseSignedClaims()` |
| **알고리즘 고정** | ✅ RS256 단일. `alg:none`·ES256 치환 거부 | ✅ `"RS256".equals(alg)` 하드체크 | ✅ jjwt 0.12.6 unsecured 기본 비활성, HS/RS 혼동 차단 |
| `iss` | ✅ | ✅ 라이브러리 기본값 | N/A |
| `aud` | ✅ `appleClientId` | ✅ **`aud` 부재 시 `isEmpty() → false`로 명시 거부** | N/A |
| `exp` / `iat` | ✅ 필수 클레임 + 만료 검증 | ✅ skew 300s | ✅ 자동 |
| 키 회전 | ✅ JWKS 캐시 5분 + refresh-ahead 30초, `jwkSet`을 필드로 재사용 | ✅ 단 verifier가 매 요청 new (M10) | N/A |
| **판정** | **안전** | **안전** | 검증은 안전, **타입 구분 부재 = C1** |

> **가장 값진 결과**: 소셜 로그인이 **클라이언트가 ID token을 제출하는 구조가 아니라
> `authCode`를 서버가 공급자 토큰 엔드포인트와 TLS로 직접 교환하는 구조**다
> (`AuthController:105` → `exchangeAuthCode`). ID token이 애초에 공격자 통제 밖에 있고,
> 그 위에 서명·`iss`·`aud`·`exp`까지 실제로 검증된다.
> `nonce` 미검증은 이 구조에서 리플레이 리스크가 사실상 없어 **이슈로 올리지 않았다.**

**하자드 리스트 통과 항목**: #1(Apple 검증), #2(Google 검증), #3(클레임 조작),
#7(DebugTokenHolder), #10(제3자 부활), #11(사용자 열거), #12(동시 가입) — **전부 통과.**

---

## 확정 요건 1 — 탈퇴 시 소셜 연동 해제가 반드시 성공해야 한다

> **제품 요건**: 사용자가 탈퇴하면 Google/Apple 쪽 앱 연동도 실제로 해제되어야 한다.

### 현재 상태: **요건 미충족 — 그리고 revoke가 아예 실행되지 않고 있다** 🔴

호출 사슬을 전수 확인한 결과:

```
revokeToken ← UserDeletionService.processUserDeletion
            ← AdminService.hardDeleteOldUsers / hardDeleteUsersImmediately
            ← AdminController (수동 API가 유일한 진입점)
```

`@Scheduled`가 코드베이스에 0건이고(스코프 4 I2), **개발 서버에는 cron도 설정돼 있지 않음이
확인됐다.** 따라서 **탈퇴한 모든 사용자의 소셜 연동이 해제되지 않고 남아 있다.**
재가입 계정만의 문제가 아니다. 부수적으로 `is_deleted = 1` 행도 계속 쌓인다.

### 필요한 작업 3가지와 그 관계

I4 수정(`socialRefreshToken` null 허용)은 **NPE만 없앴을 뿐 이 요건을 만족시키지 못한다.**
오히려 실패가 더 조용해졌다 — 이전에는 가입 시점에 500으로 시끄럽게 터졌지만,
이제는 가입이 성공하고 **며칠 뒤 탈퇴 시점의 로그 한 줄로만** 드러난다.

**실패 연쇄 (확인 완료):**

1. Google 재가입 → `refresh_token` 없음 → `socialTokenService.saveOrUpdateToken` 미호출
   → **`social_tokens` 행 없음**
2. 탈퇴 → `BaseAuthService.revokeToken:43-44`
   ```java
   SocialToken socialToken = socialTokenRepository.findByUserId(userId)
           .orElseThrow(() -> new AuthException("... 소셜 리프레시 토큰이 없습니다."));
   ```
3. `UserDeletionService:31-37`
   ```java
   catch (AuthException e) { log.error(...); }
   finally { userService.deleteUserHardlyByUserId(user.getId()); }   // ← 무조건 삭제
   ```
4. **유저는 삭제되고 소셜 연동은 영구히 남는다.** 재시도 근거(`user_id`, refresh token)도
   CASCADE로 함께 소멸해 복구 불가.

`@Retryable`은 `IOException`만 잡으므로 이 경로는 **재시도조차 되지 않는다.**

| | 없으면 | 하면 | 성격 |
|---|---|---|---|
| **C. 배치 실행** | revoke가 **한 번도 실행되지 않는다** | 탈퇴 파이프라인이 돌기 시작 | **선행 조건** — 없으면 A·B가 무의미 |
| **A. 클라이언트 재동의** | 재가입 계정은 revoke할 **크리덴셜 자체가 없다** | 정상 경로에서 revoke 성공 | **필수** — B로 대체 불가 |
| **B. 서버 안전망** | 일시 실패 한 번에 **영구 유실**(기록도 없음) | 실패해도 재시도 가능 | **보증에 필요** |

**셋 다 필요하고, 서로 대체되지 않는다.**
- C 없이는 A·B를 해도 실행이 안 된다
- A 없이는 재가입 계정을 **B로도 못 고친다** — 재시도할 크리덴셜이 없기 때문
- B 없이는 "보증"이 아니라 "운 좋으면 성공"이다

### 수정 C — 배치 실행 (선행 조건)

`@EnableScheduling` + `@Scheduled(cron = ...)`을 `hardDeleteOldUsers`에 붙인다.

⚠️ **운영 서버 확인 필요**: 라즈베리 파이 cron이 `/admin/users`를 호출하고 있다면
`@Scheduled` 추가 시 **이중 실행**이 된다. cron을 없애고 `@Scheduled`로 통일하거나,
설정 플래그로 하나만 켜야 한다.

⚠️ **C1 수정의 부작용**: `typ` 클레임이 없는 기존 토큰이 전부 거부되므로, cron이 쓰는
**ADMIN 토큰도 재발급이 필요하다.** 배포 후 cron이 조용히 401을 받기 시작할 수 있다.

### 수정 A — 클라이언트 (근본 원인, 서버에서 해결 불가)

Q1 답변으로 **offline access는 이미 지정돼 있음이 확인됐다**(최초 가입이 정상 동작하므로).
남은 문제는 **재동의**다: Google은 이미 동의한 계정에 대해 명시적 재동의 요청 없이는
`refresh_token`을 다시 주지 않는다.

→ 클라이언트가 `serverAuthCode`를 요청할 때 **강제 재동의 옵션**을 켜야 한다
(웹/서버 흐름의 `prompt=consent`에 해당하는 SDK 플래그).
**Apple은 code exchange마다 refresh token을 발급하므로 영향 없다 — Google 전용 문제다.**

### 수정 B — 서버 (I5, 안전망)

revoke 실패 시 하드 삭제를 보류하고 재시도해야 한다. 다만 무한 재시도를 막으려면
시도 횟수/최종 시각을 기록할 컬럼이 필요하고, 이는 **확정 정책 5(가명처리)가 요구하는
"처리 완료 플래그"(스코프 1 ⑤)와 같은 컬럼**이다.

→ **확정 정책 5 작업에 포함시킨다.** 확정 정책 5가 `deleteUserHardlyByUserId` 자체를
가명처리로 교체하므로, 지금 손대면 같은 코드를 두 번 건드리게 된다.

**실패 종류를 구분할 것:**
- `social_tokens` 행 없음 = **영구 실패**. 재시도해도 소용없으므로 삭제를 진행하되
  별도 기록을 남긴다
- HTTP/네트워크 실패 = **일시 실패**. 삭제를 보류하고 다음 배치에서 재시도

현재는 둘 다 `AuthException`이라 구분되지 않는다 — 별도 예외 타입이 필요하다.

### ⚠️ 확정 정책 5 명세 수정 — `social_tokens` 삭제 시점

확정 정책 5 §2는 가명처리 시 `social_tokens`를 **명시적으로 DELETE**하라고 규정했다.
그런데 그렇게 하면 **B의 재시도 근거가 사라진다** — revoke에 실패했는데 크리덴셜을
지워버리면 다시 시도할 수 없다.

**→ `social_tokens`는 revoke가 성공한 뒤에만 삭제한다.**
- revoke 성공 → `social_tokens` 삭제 → 가명처리 진행
- revoke 실패(일시) → **행 유지** → 다음 배치에서 재시도
- revoke 실패(영구) → 행 삭제 + 실패 기록 → 가명처리 진행

`user_tokens`는 revoke와 무관하므로 즉시 삭제해도 된다
(이미 탈퇴 신청 시점에 삭제하도록 반영됨 — develop `f5bb991`).

---

## 잘 된 점

1. **`JwtFilter`가 토큰의 `role` 클레임을 신뢰하지 않는다.** `JwtFilter:45-51`이 `subject`만
   꺼내 `loadUserByUsername`으로 **DB에서 권한을 재조회**한다. `JwtUtil.getUserRole()`은
   어디에서도 호출되지 않는다(grep 확인).
   → **클라이언트가 `role`을 조작해 권한 상승하는 경로가 구조적으로 없다.**
   이 코드베이스에서 가장 잘 된 결정이다.
2. **자가 `ROLE_ADMIN` 부여 차단** (`UserService:101-104`) — 스코프 4의 C1 hotfix가
   반영된 상태를 독립적으로 확인했다.
3. **JWT 시크릿 강도가 기동 시 강제된다.** `Keys.hmacShaKeyFor`가 256bit 미만이면
   `WeakKeyException`으로 애플리케이션이 뜨지 않는다.
4. **`DebugTokenHolder`와 `createTestToken`(1년 만료)은 완전한 dead code.** 참조 0건,
   `@Component`/`@Bean` 없음. **프로덕션 도달 불가, 무해**로 판정.
5. **부활 로그인은 제3자가 트리거할 수 없다.** `AuthController:117-119`는
   `exchangeAuthCode` 성공 이후에만 실행 — 공급자에서 유효한 code를 받아낸 본인만 도달.
6. **Refresh 회전이 구현돼 있다** (`AuthController:241-242` + `saveOrUpdateToken`이 행 덮어쓰기).
7. **에러 응답에 내부 정보가 없다.** 고정 문자열만 반환, 스택트레이스·예외 메시지 없음.
   로그인 실패가 "없는 유저"와 다른 실패를 구분하지 않음 → **사용자 열거 없음.**
8. `/actuator/**`가 permitAll 밖이라 401.
9. 동시 최초 로그인은 `UNIQUE (provider, provider_id)` → 409로 안전.
10. `/auth/token/refresh`가 `findUserById`를 거쳐 soft-delete 유저 재발급을 409로 차단.

---

## Critical

### C1 — Refresh token이 Access token으로 그대로 통용 ✅ **수정 완료** (`f5bb991` + `f990d5b`)

**원장 관리자 직접 확인 완료.**

```java
// JwtUtil — createAccessToken
.subject(userId).claim("role", ...).claim("username", ...)
// JwtUtil — createRefreshToken  ← 타입 구분 클레임 없음
.subject(userId).issuedAt(...).expiration(...).signWith(key)

// JwtFilter:44-51 — 서명·만료만 검사하고 권한은 DB에서 붙인다
if (jwtUtil.isValidToken(token)) {
    Long userId = jwtUtil.getUserId(token);
    UserDetails userDetails = customUserDetailsService.loadUserByUsername(String.valueOf(userId));
    ... setAuthentication(...);
}
```

두 토큰이 **같은 키로 서명되고 같은 `subject`를 담으며 `typ` 구분이 없다.**
`JwtFilter`가 유일한 진입점이고 `user_tokens` 대조도 하지 않는다.
→ **refresh token을 `Authorization: Bearer`로 보내면 access token과 완전히 동일하게 동작한다.**

**산술** (`application.properties:39,41` 확인):
- access `1,200,000ms` = **20분**
- refresh `604,800,000ms` = **7일 = 10,080분 = access의 504배**

**공격 시나리오** — 출발점: **refresh token 문자열 1개**(백업된 앱 저장소, 중고폰,
프록시 로그 — access token보다 오래 남아 유출 표면이 넓다):
1. 피해자 **로그아웃** → `logout`은 FCM 토큰만 지움(I2). 공격자 토큰 유효
2. 피해자 **재로그인** → 회전 발생. `/auth/token/refresh`에서는 옛 토큰 거부
3. **그런데 공격자는 그 엔드포인트를 쓸 필요가 없다.**
   `GET /users/profiles` `Bearer <옛 refresh token>` → 서명·exp 통과 → **피해자 권한으로 200**
4. 피해자가 **탈퇴**해도 C2 때문에 여전히 통과

**결과: 발급 시점 기준 7일 동안 로그아웃·회전·탈퇴 그 무엇으로도 취소 불가능한
전권 베어러 크리덴셜.** 회전 로직이 통째로 무력화된다.

**수정**: `createRefreshToken`에 `.claim("typ","refresh")`(access는 `"access"`),
`JwtFilter`에서 `"access"`가 아니면 거부, `/auth/token/refresh`는 `"refresh"`만 수용.
기존 토큰 호환이 필요하면 access 클레임을 먼저 배포하고 필터 가드를 나중에 켠다.

### C2 — Soft-delete 유저가 인증을 그대로 통과 ✅ **수정 완료** (`f5bb991`)

```java
// UserRepository:19-20 — is_deleted 조건 없음
@Select("SELECT * FROM users WHERE id = #{id}")
Optional<User> findById(Long id);

// CustomUserDetailsService:29-32 — is_deleted 검사 전혀 없음
User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
return new CustomUserDetails(user);
```

`deleteUserSoftlyByUserId`는 `is_deleted=1`만 세팅하고 **`user_tokens` 행을 지우지 않는다.**
`domain/workplace`, `domain/routine`, `domain/alarm` 전체에 `isDeleted()` 참조가 **0건**이다.

**시나리오 (반대 방향이 더 나쁘다)**: 계정 탈취를 당했을 때 "탈퇴"가 유일한 자구책인데,
**탈퇴해도 공격자 토큰이 죽지 않는다.**

**수정**: `CustomUserDetailsService`에서 `user.isDeleted()`면 `UsernameNotFoundException`.
**경계에서 한 번에 차단**하는 것이 호출자마다 가드를 다는 것보다 diff가 작고 누락이 없다.

> **C1 × C2 = 이 서버에는 현재 동작하는 크리덴셜 취소 메커니즘이 존재하지 않는다.**

---

## Important

| # | 내용 |
|---|---|
| **I1** | **Swagger/OpenAPI가 프로덕션에 무인증 노출.** `SecurityConfig:27-30,48` + `// TODO: 나중에 swagger 비활성화 하기`. `resources/`에 `application.properties` 하나뿐이고 프로파일 분리 없음. 익명으로 `GET /v3/api-docs` 한 방에 전체 API 표면·관리자 경로·역할 체계 획득 → 이후 모든 공격의 정찰 비용 0 |
| **I2** ✅ `f5bb991` | **로그아웃이 서버 상태를 무효화하지 않는다.** `UserService:204-208`이 FCM 토큰만 지움. `user_tokens` 행이 남아 refresh가 **로그아웃 후에도 7일 유효**하고 C1으로 access처럼 쓰인다. **수정**: `logout`에서 `user_tokens` 삭제 — 실질 노출 창이 7일 → 20분으로 |
| **I3** ⚠️ 부분 `f5bb991` | (a)는 재발급 타입 가드로 해소됨. (b) `AuthException` 500은 미수정. **인증 실패가 401이 아니라 500.** (a) `UserTokenService:45-46`의 `getUserId`가 예외를 삼키지 않아 만료·변조 refresh → 500 + `logger.error`. 익명 공격자가 쓰레기 토큰으로 ERROR 로그 무제한 생성 가능. (b) `AuthController:96-97`이 `throws AuthException`(**checked**)인데 `GlobalExceptionHandler`는 `RuntimeException`만 처리 → 잘못된 `authCode` → 500 |
| **I4** ✅ `f5bb991` | **`startCreateUser` NPE — 재가입 경로가 500으로 막힘.** `UserService:64-68`의 `socialRefreshToken.isEmpty()`에 null 체크 없음. 같은 흐름의 로그인 분기(`AuthController:122`)는 **제대로 막고 있다** — 신규 가입만 누락. Google은 `refresh_token`을 최초 동의 시에만 발급하므로 **"탈퇴 후 재가입"에서 정통으로 터진다. 확정 정책 5의 선행 조건** |
| **I5** ✅ `7706fe4`+`58dae8a` | **소셜 revoke 실패가 조용히 삼켜짐.** `UserDeletionService:26-37`이 `finally`에서 성공 여부 무관하게 하드 삭제 → 재시도 근거가 CASCADE로 소멸 → **소셜 grant 영구 잔존.** 사용자는 탈퇴했다고 믿지만 Apple/Google에는 연동이 남는다. `@Retryable`이 `IOException`만 잡아 HTTP 4xx는 재시도조차 안 됨 |
| **I6** | **`user_tokens`/`social_tokens`에 `UNIQUE (user_id)` 없음.** read-then-write 패턴이라 동시 로그인 시 행 2개 → `Optional<UserToken>`에 2행 → `TooManyResultsException` → **해당 유저 로그인·재발급 영구 500.** 스코프 5 C-2와 동일한 결함 유형 |
| **I7** | **Refresh token과 소셜 refresh token이 DB 평문 저장.** 자체 refresh는 C1 때문에 전권 크리덴셜이라 DB 읽기 권한만으로 전 사용자 로그인 가능. **수정**: 자체 refresh는 SHA-256 해시 저장 후 비교(검증이 `.equals()` 한 줄이라 변경 폭 작음), 소셜은 AES-GCM |

---

## Minor (요약)

**M1 — `JwtUtil:28`의 `log.debug(secretKey)`.** 현재 root INFO라 출력되지 않지만
`logging.level.com.moup=DEBUG` 한 줄이면 **전 사용자 위조 가능한 키가 로그에 박힌다.**
제거 1줄, 리스크는 시스템 전체. **즉시 삭제 권장.**

그 외: CORS 와일드카드 + credentials(현재 Bearer 전용이라 무해하나 쿠키 도입 시 즉시 취약) ·
`UserTokenService:27`의 `System.out.println` · `JwtFilter:58`의 도달 불가 catch 분기 ·
`JwtFilter`가 토큰을 2회 파싱(요청당 HMAC 2회) · dead code 삭제 권장
(`DebugTokenHolder`, `createTestToken`, `getUsername`, `getUserRole`) ·
`AuthServiceFactory.getService`가 null 반환 가능(DB ENUM에 `LOGIN_NAVER`/`LOGIN_KAKAO`가
있는데 Java `Login` enum에는 없음) · `/home/**`이 매처에 없어 `authenticated()`로 폴백 ·
`AppleJwtUtil`의 deprecated `setHeaderParam` · `GoogleIdTokenVerifier` 매 요청 생성

---

## 확정 정책 5(가명처리) 전환 영향 분석 ⚠️ 중요

리뷰어가 별도 분석한 결과, **지금 상태로 배포하면 안 된다.**

### ① 재가입은 "새 계정"으로 정상 동작 — 단 I4를 고쳐야 한다

`AuthController:110`의 `findByProviderAndId`가 매칭되지 않아 신규 가입 분기로 간다.
`UNIQUE` 충돌도 없다 — **설계 의도대로다.** 그러나 재가입은 정의상 "Google이 이미
동의를 받은 계정"이라 `refresh_token`이 `null`로 올 확률이 높고 I4의 NPE에 부딪힌다.
**I4는 확정 정책 5의 선행 조건이다.**

### ② 부활 경로가 두 가지 동작으로 갈린다 — 명시적 결정 필요

`provider_id`가 난수로 바뀌면 부활 조건이 성립하지 않는다. 그런데 **가명처리 시점 이전
(유예 3일 내)에는 원래 `provider_id`가 남아 부활이 동작한다.**
→ **"유예기간 내 = 부활, 유예기간 후 = 새 계정"** 이라는 두 동작이 생기며 코드
어디에도 명시돼 있지 않다. **의도된 UX인지 확인하고 문서화할 것.**

### ③ 토큰 테이블이 자동 정리되지 않는다 — 가장 위험 🔴

`social_tokens`/`user_tokens`는 `ON DELETE CASCADE`로 정리되는데(`moup.sql:28,38`),
**`users` 행을 남기면 CASCADE가 발동하지 않는다.** 결과:
- `social_tokens.refresh_token`(Apple/Google 실 크리덴셜) **영구 잔존**
- `user_tokens.refresh_token` 영구 잔존 → **C1과 결합하면 "탈퇴한 계정의 전권 토큰이
  7일간 계속 통용"**

→ **가명처리 로직은 두 테이블을 명시적으로 DELETE해야 한다.**
(확정 정책 5 §2에 이미 포함돼 있음 — **독립 확증**)

### ④ 가명처리된 행이 그대로 인증된다

C2 때문에 `id`만 맞으면 `CustomUserDetails`가 만들어진다. 게다가 `username`을 비우면
`CustomUserDetails.getUsername()`이 `null`을 반환하는데 `UserDetails` 계약상 허용되지
않는다. **C2 수정이 확정 정책 5의 선행 조건이다.**

### ⑤ 하드 삭제 배치가 같은 행을 무한 반복 처리한다 🆕

`findAllOldHardDeleteUsers`는 `is_deleted = 1 AND deleted_at < ...`로 대상을 고른다.
가명처리 후에도 `is_deleted = 1`을 유지하면 **이 배치가 매번 같은 행을 반복 처리**하며
`processUserDeletion`(→ 이미 없는 social token → `AuthException` → 로그 폭탄)을 계속 돈다.
**가명처리 완료를 표시하는 플래그나 `deleted_at` 기준 재정의가 필요하다.**

> ### 확정 정책 5 전환 전 필수 선행 작업
> **I4 → C2 → (social_tokens/user_tokens 명시적 삭제) → ⑤ 배치 조건 재정의**

---

## 확인 질문

| # | 질문 |
|---|---|
| ~~**Q1**~~ | **답변 완료.** 최초 가입이 정상 동작하므로 offline access는 **이미 지정돼 있다.** 문제는 **재동의**로 한정된다 — Google은 이미 동의한 계정에 `prompt=consent` 없이는 `refresh_token`을 재발급하지 않는다. 아래 "확정 요건 1" 참조 |
| ~~**Q2**~~ | **답변 완료 → 빈 값으로 정상 동작 확인됨.** 네이티브 앱 코드 흐름에서 Google이 `redirect_uri` 검증을 요구하지 않는 클라이언트 타입. **보안 이슈 없음으로 종결.** 다만 `application.properties`의 `google.redirect.uri`는 미사용 설정이므로 제거 권장 |
| **Q3** | `hardDeleteOldUsers`를 호출하는 스케줄러가 없다(`@Scheduled` 0건). **라즈베리 파이에 cron이 설정돼 있을 가능성** — 확인 필요(`crontab -l`). 있다면 그 cron이 쓰는 **ADMIN 토큰의 저장 위치와 갱신 방식**이 검토 대상이다. C1 수정으로 `typ` 없는 기존 토큰이 거부되므로 **cron의 토큰도 재발급이 필요할 수 있다** → 스코프 7에서 배포 스크립트와 함께 확인 |

---

## 테스트 우선순위

1. **`JwtFilter`에 refresh token을 Bearer로 → 401** (C1). **이번 리뷰에서 가장 가치 있는 테스트**
2. soft-delete 유저 토큰으로 `/workplaces/**` → 401 (C2)
3. 로그아웃 후 이전 refresh로 `/auth/token/refresh` → 401 **+ Bearer로도 401** (I2 + C1)
4. 만료·변조 refresh → 500이 아니라 400/401 (I3-a)
5. 잘못된 `authCode` → 500이 아니라 401 (I3-b)
6. `socialRefreshToken = null`로 `startCreateUser` → NPE 없이 정상 가입 (I4, **정책 5의 게이트**)
7. 동일 user_id로 `saveOrUpdateToken` 동시 호출 → 행 1개 (I6)
8. `prod` 프로파일에서 `GET /v3/api-docs` → 401/404 (I1)
9. `completeCreateUser`에 `role="ROLE_ADMIN"` → 400 (hotfix 회귀 방지)
10. Apple ID token: `alg:none`, 다른 키 재서명, `aud` 불일치 → 전부 거부 (명세 고정용)

---

## 총평

**토큰 검증 계층은 견고하고 토큰 수명주기 계층은 그렇지 않다.**

검증 쪽은 칭찬할 만하다. 소셜 로그인을 authorization code 교환 방식으로 설계해
클라이언트가 ID token을 위조할 여지를 **구조적으로** 제거했고, 그 위에 Apple은
JWKS + RS256 고정 + 클레임 전수 검증, Google은 서명 검증 + `aud` 필수(빈 `aud`도 거부)까지
실제로 동작한다. 무엇보다 **`JwtFilter`가 `role` 클레임을 아예 읽지 않고 DB에서 권한을
재조회**하는 것 — 이 하나로 가장 흔한 권한 상승 경로가 통째로 없다.

문제는 발급 이후다. Access와 refresh가 서명 키·`subject`·구조까지 동일하고 타입 구분이
없어 refresh가 7일짜리 전권 Bearer로 통용되고(C1), 인증 경계가 계정 삭제 상태를 확인하지
않아(C2) 로그아웃·회전·탈퇴 세 무효화 수단이 **동시에** 무력화된다.

**두 이슈 모두 수정은 작다** — 클레임 1줄 + 필터 가드 1개, `is_deleted` 체크 1줄.
진짜 비용은 코드가 아니라 "고쳐야 한다는 사실을 아는 것"이었다.
