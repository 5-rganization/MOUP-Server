# 적용된 수정 이력

리뷰 도중 예외적으로 **develop에 직접 반영한** 수정들. 나머지는 원칙대로 리뷰 완료 후
`fix/code-review-findings`에서 일괄 처리한다.

**예외 적용 기준**: 즉시 악용 가능한 보안 결함이거나, 사람이 실제로 겪어 보고한 파손이면서
수정이 좁고 다른 스코프의 리뷰 대상 로직을 바꾸지 않는 경우.

> ⚠️ **아직 푸시하지 않았다.** 배포 전 아래 "배포 체크리스트"를 반드시 확인할 것.

---

## 배포 체크리스트 🔴

이 수정들 때문에 **배포 시 반드시 필요한 조치**가 있다.

| # | 조치 | 안 하면 |
|---|---|---|
| 1 | **`ADMIN_AUTH_TOKEN` 재발급 후 GitHub secret 갱신** | 배치 cron이 **401을 받고 멈춘다.** 하드 삭제와 소셜 연동 해제가 전부 중단된다 |
| 2 | 전 사용자 1회 재로그인 안내 | 기존 발급 토큰에 `typ` 클레임이 없어 전부 거부된다 |
| 🔴 3 | **Firebase 서비스 계정 키 폐기·재발급** — 배포와 무관하게 **지금 당장.** [스코프 7 C7](scope-7-infra.md) | Docker Hub `neoskycladdocker/moup`가 **public**임이 확인됐고(`is_private: False`) 키가 이미지 안에 있다. 누구나 `docker pull`로 꺼낼 수 있다 |
| 4 | **`db/migrations/2026-08-27-workplaces-owner-set-null.sql`를 운영 DB에 수동 적용** (앱 배포 **후**) | 스키마 파일만 고쳐서는 기존 DB가 그대로다. 사장님 탈퇴 시 알바생 근무·급여가 계속 CASCADE 삭제된다 |

`ADMIN_AUTH_TOKEN`은 `JwtUtil.createTestToken`(1년 만료)으로 발급한다. 이 메서드는 런타임
호출자가 없어 dead code처럼 보이지만 **cron 크리덴셜의 수동 발급 도구**다
(`.github/workflows/deploy.yml:57` → `.env`, 서버 코드에서 읽는 곳은 없음).

---

## 커밋별 내역

### `219a8e4` — 사장님이 알바생 근무 상세를 조회할 수 없던 문제

**스코프 2 C2.** `RoutineService.java:406`이 `findByIdAndUserId`로 근무자를 조회해 사장님의
`userId`로는 찾지 못하고 404가 났다. `:412`의 사장님 허용 분기는 도달 불가 dead code였다.
`findById`로 바꿔 판정을 기존 권한 검사에 위임.

### `d2a6021` — 권한 상승과 교차 테넌트 급여 수정 차단

**스코프 4 C1** — 신규 가입자가 `role`에 `ROLE_ADMIN`을 넣어 자가 부여할 수 있었다.
`/auth/**`가 permitAll이고 `/admin/**`가 `hasRole("ADMIN")`이므로,
`DELETE /admin/users/immediate`로 삭제 대기 중인 전 유저를 유예기간 무시하고 영구 삭제하거나
전 사용자에게 푸시를 보낼 수 있었다.
→ `completeCreateUser`에서 `ROLE_WORKER` / `ROLE_OWNER`만 허용.

**스코프 4 C2** — `updateWorkerForOwner`가 `workplaceId` 소유권만 검증하고 `workerId`가 그
근무지 소속인지 확인하지 않았다. 사장님이 무관한 근무지 근무자의 시급·4대보험·주휴·야간수당
플래그를 임의 재작성하고 `estimated_net_income`까지 덮어쓸 수 있었다.
→ 권한 검증 **직후** `existsByIdAndWorkplaceId` 확인 (순서를 뒤에 둬 404/403 열거도 차단).

### `f5bb991` — 토큰 타입 구분과 탈퇴 유저 인증 차단

**스코프 1 C1** — `createRefreshToken`에 타입 구분 클레임이 없고 `JwtFilter`가 서명·만료만
검사해, **refresh token을 Bearer로 보내면 access token과 동일하게 동작**했다
(access 20분 vs refresh 7일 = 504배). 로그아웃·회전·탈퇴 어느 것으로도 취소되지 않는
전권 크리덴셜이었다.
→ `typ` 클레임(access/refresh) 추가, `JwtFilter`는 access만, 재발급은 refresh만 수용.

**스코프 1 C2 / 스코프 4 I1** — `CustomUserDetailsService`가 `is_deleted`를 검사하지 않아
탈퇴 신청 유저가 유예기간 내내 정상 인증됐다. 계정 탈취 시 "탈퇴"라는 자구책이 무력화됐다.
→ 인증 경계에서 차단.

> **확정 정책 8 (스코프 4 Q2)**: **탈퇴 유예기간 중에는 재로그인 전까지 전면 차단한다.**
> 탈퇴 신청 즉시 401이며, 유예기간 3일 내 소셜 재로그인 시 자동 복구된다.
> 복구는 `/auth/login`(permitAll)의 소셜 재인증으로 이루어지므로 이 차단에 영향받지 않는다.
> 이 수정이 정확히 그 동작이다.

**스코프 1 I4 / 스코프 4 I7** — `startCreateUser`의 `socialRefreshToken.isEmpty()`에 null
검사가 없어 Google 재가입 시 NPE. → null 허용 (로그인 분기와 동일).

**스코프 1 I2** — 로그아웃이 FCM 토큰만 지워 refresh가 7일간 유효했다.
→ `UserTokenRepository.deleteByUserId` 추가 후 로그아웃·탈퇴 신청 시 폐기.

**스코프 1 I3-a 부수 해소** — 재발급 경로에 타입 가드를 넣어, 만료·변조 토큰이
`getUserId`에서 예외를 던져 500이 되던 문제도 함께 해결됐다.

### `f990d5b` — 배치 cron의 장기 토큰 회귀 수정

`f5bb991`이 만든 회귀. `createTestToken`이 `typ`를 달지 않아 이 메서드로 발급한
`ADMIN_AUTH_TOKEN`이 거부되는 상태였다. 그대로 배포하면 cron이 401을 받아 하드 삭제와
소셜 연동 해제가 완전히 멈춘다. → `createTestToken`에도 `typ=access` 추가.

### `7706fe4` — 소셜 연동 해제에 성공했을 때만 유저 삭제

**스코프 1 I5.** `processUserDeletion`이 `finally`에서 revoke 성공 여부와 무관하게 삭제해,
실패해도 유저가 사라지고 재시도 근거인 `social_tokens`도 CASCADE로 소멸했다.
**소셜 연동이 영구히 남았다.**
→ 성공 시에만 삭제. 실패하면 `is_deleted = 1`로 남아 다음 배치가 재시도.
공급자 서비스가 없는 경우(`getService`가 null)는 영구 실패이므로 삭제 진행 + 로그.

### `58dae8a` — 소셜 연동 해제 재시도에 30일 상한

`7706fe4`가 만든 문제. 종료 조건이 없어 영구 실패 계정의 데이터가 무한정 남았다.
→ `user.delete.revoke-give-up-period=30`. 배치를 두 갈래로 분리:

| 탈퇴 경과 | 재시도 | 포기 |
|---|---|---|
| ~3일 (유예) | – | – |
| 4일 ~ 30일 | ✅ | – |
| 31일 이상 | – | ✅ 기록 후 삭제 |

두 조건은 서로소이며 합치면 기존 범위와 같다. 경계값으로 확인했다.
포기 시 `ERROR` 로그만 남긴다 — **이 로그가 수동 조치의 유일한 단서이므로 알림 권장.**
별도 기록 테이블은 가명처리 작업 때 함께 검토한다.

### `1008aea` — `@PreAuthorize` 인가 거부가 403으로

**스코프 7 C1.** `GlobalExceptionHandler`에 catch-all `RuntimeException` 핸들러만 있고
`AccessDeniedException` 핸들러가 없었다. Spring Security 6의 메서드 보안이 던지는
`AuthorizationDeniedException`은 `AccessDeniedException` → `RuntimeException` 하위이고
컨트롤러 프록시 안에서 발생하므로, catch-all이 먼저 잡아 **500을 반환하고 정상 종료**했다.
그래서 `SecurityConfig`의 `accessDeniedHandler`는 예외를 보지도 못했다.

`@PreAuthorize`가 걸린 **18개 엔드포인트**의 인가 거부가 전부 서버 오류로 위장되고 있었고
각 컨트롤러의 `@ApiResponse(responseCode = "403")` 문서와도 어긋났다.
→ `AccessDeniedException` 핸들러 추가(catch-all보다 구체적이므로 우선 매칭).

---

### `f3feff7` — 탈퇴로 NULL이 된 ID 비교 시 500 → 403

**스코프 7 C2 (스코프 2 I3 · 스코프 4 #12의 세 번째 독립 확증).**
`workplaces.owner_id`와 `workers.user_id`는 스키마상 NULL을 허용하고
`workers.user_id`는 `ON DELETE SET NULL`이다. 탈퇴한 사용자의 행에서
`Long.equals()`를 직접 호출하면 NPE가 나고 catch-all이 500을 반환했다.

`Objects.equals`로 바꿔 NULL을 "불일치"로 취급(fail-closed)한다.

| 위치 | 비고 |
|---|---|
| `PermissionVerifyUtil:10,17` | 호출자 17곳의 공통 경로 |
| `WorkplaceService:253` `deleteWorkplace` | 원장에 없던 신규 발견 |
| `WorkService:355` `getAllWorkByWorkplace` | 원장에 없던 신규 발견 |

> `RoutineService:412`는 `userId.equals(...)` 형태로 **요청자 ID가 좌변**이라
> NPE가 나지 않는다. 수정 대상이 아니다.

### `98ac8e9` — `workplaces.owner_id` CASCADE → SET NULL

**스코프 7 C3 (스코프 4 C4 · 스코프 5 I-8의 세 번째 독립 확증).**
사장님이 하드 삭제되면 `users` → `workplaces` → `workers` →
`works`/`salaries`/`work_routine_mappings`가 연쇄 삭제됐다. cron이 프로덕션에서
실제로 돌고 있으므로 **이미 발현 중인 결함**이었다.

- `db/moup.sql`, `db/init/moup.sql` — FK에 `fk_workplaces_owner` 이름을 부여하고
  `SET NULL`로 변경. **신규 설치용이며 기존 DB에는 적용되지 않는다.**
- `db/migrations/2026-08-27-workplaces-owner-set-null.sql` — 운영 DB에 수동 적용할 `ALTER`.

⚠️ **기존 FK 이름이 스키마에 없어 MySQL이 자동 생성했다.** 마이그레이션 파일은
`workplaces_ibfk_1`을 가정하며, 실행 전 `information_schema`로 확인하는 쿼리를
주석으로 함께 담았다.

---

## 추가된 테스트 (34건)

| 파일 | 건수 | 목적 |
|---|---|---|
| `SalaryCalculationServiceTest` | 11 | 야간 경계 8 + 멱등성 2 + 복합 1. **수정 전 회귀 고정용** |
| `JwtFilterTokenTypeTest` | 7 | C1 — 실제 필터에 실제 토큰을 통과시켜 검증 |
| `CustomUserDetailsServiceTest` | 3 | C2 — 탈퇴 유저 차단 |
| `UserDeletionServiceTest` | 5 | revoke 성공/실패/네트워크오류/공급자없음/포기 |
| `GlobalExceptionHandlerTest` | 3 | C1 — 인가 거부 403, 그 외 RuntimeException 500 |
| `PermissionVerifyUtilTest` | 5 | C2 — owner_id/user_id가 NULL일 때 NPE 대신 403 |

**변이 테스트로 실효성 확인**: 야간 경계에서 `equals(22:00)` 제거 → 5건 실패,
`grossIncome` 누적으로 변경 → 멱등성 2건 실패. C1은 수정 전 실제로 통과함을 확인한 뒤
기대값을 뒤집었다.

> ⚠️ 기존 `WorkerServiceTest` / `WorkplaceServiceTest` 6건은 **이 작업 이전부터**
> Mockito `InvalidUseOfMatchersException`으로 실패 중이다(스텁 메서드에 파라미터가 늘었는데
> 테스트가 미갱신). 즉 `./gradlew test`는 develop에서 원래 BUILD FAILED다.

---

## 신규 발견 — 스코프 7로 이관 (✅ 리뷰 완료, [scope-7-infra.md](scope-7-infra.md)에서 다룸)

수정 과정에서 배포 설정을 보다가 나온 것들. **스코프 7 리뷰가 전부 독립 확증했다** —
INF-1·2·5는 C5, INF-3은 Minor, INF-4는 `.env` 소비처 분석에서.

| # | 내용 |
|---|---|
| INF-1 | **cron 스크립트 경로 오류.** `delete_old_users.log`(git 추적 중)에 `/home/neoskyclad/MOUP-Server/src/main/resources/delete_old_users.sh: No such file or directory`. 실제 구조는 `MOUP-Server/server/src/main/resources/`로 **`server/`가 빠져 있다.** `deploy.yml`의 (주석 처리된) crontab 설정에도 같은 경로가 박혀 있다 |
| INF-2 | **`delete_old_users.sh`가 git에 없다.** 라즈베리에만 존재해 무슨 일을 하는지 검증할 수 없고 서버 교체 시 재현 불가. 소셜 연동 해제라는 중요한 파이프라인이 버전 관리 밖에 있다 |
| INF-3 | **`delete_old_users.log`가 git에 추적되고 있다.** 로그 파일은 커밋 대상이 아니다 |
| INF-4 | **1년 만료 ADMIN 토큰이 `.env`에 평문 상주.** 유출 시 관리자 권한 전체. 만료·회전 정책이 없다 |
| INF-5 | crontab 설정이 `deploy.yml`에서 주석 처리돼 있어 **배포가 cron을 관리하지 않는다.** 라즈베리에 수동 설정된 상태 |

---

## 아직 수정하지 않은 것 (원칙대로 일괄 처리 대상)

리뷰 완료 후 파일 단위로 묶어 `fix/code-review-findings`에서 처리한다.
각 스코프 문서의 finding 목록 참조.

**🔴 남은 최우선은 C7(Firebase 키 회전)뿐이다** — 코드가 아니라 운영 조치.
Docker Hub `neoskycladdocker/moup`가 public이고 키가 jar 안에 들어간다.

**설계 작업이 필요한 것**: 스코프 4 C3(`is_accepted` 게이트) · C4(가명처리) ·
스코프 3 C-1(주휴수당 산식) · C-6(범위 재계산 API) · 스코프 5 C-1/C-2/C-3 ·
**스코프 7의 마이그레이션 절차 부재**(가명처리·`withdrawn_at` 작업을 직접 막는다)

**한 줄~몇 줄이지만 아직 안 한 것**: 스코프 1 M1(`log.debug(secretKey)`) ·
I1(Swagger 프로덕션 노출) · I6(`user_tokens` UNIQUE) · 스코프 5 I-4(`SecureRandom`) ·
스코프 4 I3(라벨 색상 무동작)
