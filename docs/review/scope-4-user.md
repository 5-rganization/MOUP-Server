# 스코프 4 — 사용자(User) · 알바생(Worker) 도메인

- **범위**: `server/src/main/java/com/moup/domain/user/` 전체 ~2,590 LOC (DTO 42개 포함)
- **판정**: ~~**아니오** — 리뷰 전체에서 유일하게 "병합 불가"~~ → **해소.** C3(`is_accepted` 미검사)는 [Phase 2](fix-plan.md#-phase-2--완료-f5f8cba), C4(사장님 탈퇴 시 데이터 보존)는 [Phase 6](fix-plan.md#-phase-6--완료-abf48ee), C5(휴게시간 하한)는 [Phase 0-2](fix-plan.md#0-2-음수-resttimeminutes-하한-2-c1--3-c-5--4-c5--135b966)에서 수정됐다
- **집계**: Critical 5 / Important 10 / Minor 11 / 확인 질문 5
- **리뷰 격리**: `docs/review/` 차단, 확정 정책 4건만 전제로 제공

---

## 교차 검증

| 스코프 4 | 이전 | 판정 |
|---|---|---|
| **C3** `is_accepted` 미검사 | 스코프 2 I2, 스코프 5 C-1 | **3번째 독립 확증.** 세 리뷰어가 서로 모른 채 같은 결론 |
| **C5** 음수 `restTimeMinutes` | 스코프 2 C1, 스코프 3 C1 | **3번째 독립 확증.** 스코프 4가 `domain/user/dto` 쪽 2개 DTO를 담당 |
| **I5** 동료 급여 노출 | 스코프 2 I1 | 독립 확증 |
| **I6** `SELECT *` 위치 기반 매핑 | 스코프 5 I-9 | **확증 + 확장.** `User`만 `@NoArgsConstructor`가 있고 `Worker`/`Workplace`/`Salary`/`Work` 4개가 취약함을 특정 |
| **I8** 트랜잭션 내 외부 호출 | 스코프 2 C5, 스코프 5 I-6 | 확증 + 신규 지점 3개 |
| **I10** 사장님 인건비 기준 | 스코프 3 Q8 | 확증 + 산수 (약 17% 과소 표시) |

### 자체 검증 (원장 관리자)

**C1 확인**: `AuthController.java:202`의 `Role.valueOf(registerRequest.getRole())` —
`RegisterRequest.role`은 검증 없는 `String`, `Role` enum에 `ROLE_ADMIN` 포함,
`UserService.completeCreateUser:93-108`에 역할 화이트리스트 없음,
`SecurityConfig:45`가 `/auth/**` permitAll, `:49`가 `/admin/**` `hasRole("ADMIN")`.
**전 경로 확인 완료.**

**C2 확인**: `WorkerService.java:225-233` — `verifyOwnerPermission`은 `workplaceId`만
검증하고 `workerId`가 그 근무지 소속인지 **한 번도 확인하지 않음**.
`salaryRepository.findByWorkerId(workerId)`는 근무지 스코프가 없음. **확인 완료.**

---

## 탈퇴 판정 신호 전수 조사 (최우선 과제)

### 핵심 발견 — "가명처리하면 깨진다"가 아니라 **"이미 깨져 있다"**

**1. `WorkerService.java:63` → SQL `AND user_id != #{excludeUserId}`**
SQL 3값 논리상 `NULL != 5` → `NULL` → 필터링된다. **탈퇴자 행은 오늘도 이미 근무자
목록에서 통째로 빠진다.** 따라서 `:81-87`의 `"탈퇴한 근무자"` 폴백은 **도달 불가능한
죽은 코드**였다. 가명처리 후에는 정반대로 탈퇴자가 정상 근무자로 섞여 나온다.

**2. `PermissionVerifyUtil.java:10`의 `workerUserId.equals(...)`**
알바생이 하드 삭제되면 사장님의 `GET/PATCH/DELETE /works/{workId}`가 전부 **NPE → 500**.
**확정 정책 1을 오늘도 정면 위반 중이다.**

### 전수 표 (행위 사이트 6 / 표시 사이트 8)

| # | 위치 | 성격 | 가명처리 후 |
|---|---|---|---|
| 1 | `WorkerService.java:63` `user_id != ?` | **행위 (이미 고장)** | 탈퇴자가 정상 근무자로 노출 |
| 2 | `WorkerService.java:81-87` 닉네임 폴백 | 표시 | 오늘 도달 불가, 이후 영구 미발동 |
| 3 | `WorkerService.java:129` `isActiveOnly` 필터 | **행위** | **탈퇴자가 "활성" 목록에 100% 포함** |
| 4 | `WorkerService.java:274-275` | **행위 — NPE** | 오늘 500, 이후 #5로 이동 |
| 5 | `WorkerService.java:279` / `WorkerRepository.java:171` | **행위 — 조용한 무동작** | **204 반환하지만 삭제 안 됨** |
| 6 | `WorkerService.java:287,291,296` `acceptWorker` | **행위** | `findUserById(null)` → 404, 승인 불가 |
| 7·8 | `WorkService.java:159-165, 172` | 표시 | 가명 닉네임 노출 |
| **9** | **`WorkService.java:182-184`** | **행위 — 최우선** | **사장님이 탈퇴자 앞으로 미래 근무·급여를 계속 생성** |
| 10 | `WorkService.java:361` `filter(nonNull)` | 방어 | 무해 |
| 11 | `WorkService.java:379, 947, 979` | 표시 | 영구 미발동 |
| **12** | **`PermissionVerifyUtil.java:10`** | **행위 — NPE, 최우선** | **오늘 이미 500** |
| 13 | `SalaryCalculationService.java:531` `filter` 누락 | 방어 | 불일치 |
| 14 | `SalaryCalculationService.java:639-640` | 표시 | 영구 미발동 |

### 역방향 — "non-null이면 활성 사용자" 가정

| 위치 | 실제 |
|---|---|
| `CustomUserDetailsService.java:29-32` | **`is_deleted` 미검사.** 소프트 삭제 유저가 유예 3일간 정상 인증되어 근무 생성·수정·삭제, 근무지 참여까지 수행 가능 |
| `FCMService.java:45-46` | `findUserById`가 `AlreadyDeletedException` 409 → **소프트 삭제된 알바생은 승인/거절 불가, 소프트 삭제된 사장님의 근무지는 아무도 참여 불가** (트랜잭션 전체 롤백) |

### 리뷰어 권고 — `workers.withdrawn_at` 신설 ✅ 채택 권장

`users.is_deleted`에 의존하지 말고 **`workers`에 `withdrawn_at DATETIME NULL` 컬럼을
신설**할 것. 이유:

1. **계정 탈퇴와 "이 근무지를 그만둠"은 다른 사건이다** — 알바생이 A매장을 그만두고
   B매장에서 계속 일할 수 있어야 한다
2. 근무자 목록·근무 생성 쿼리가 **`users` 조인 없이** 필터링 가능
3. 가명처리·소프트삭제·복구 **어느 상태와도 독립**

`user_id`는 순수 FK로 되돌린다.

> **원장 관리자 주석**: 이 설계가 앞서 스코프 5에서 제안한 `users.is_deleted` 검사보다
> 낫다. 확정 정책 5(가명처리)의 함정 2를 근본적으로 해소한다.

---

## 잘 된 점

- **역할 게이트 문법이 정확하다.** `@PreAuthorize("hasRole('ROLE_OWNER')")`와
  `SecurityConfig`의 `hasAnyRole("WORKER",...)`가 정합. `ROLE_ROLE_` 이중 접두사 함정에
  빠지지 않았다. **확인 완료.**
- **N+1 방지가 진지하다.** `getWorkerList`/`getActiveWorkerList`,
  `getWorkerMonthlyWorkplaceSummaryList`(쿼리 3개 고정),
  `getOwnerMonthlyWorkplaceSummaryList`(쿼리 5개 고정) — 근무자·근무지 수와 무관하게 상수.
- **빈 컬렉션 처리**로 MyBatis `IN ()` 문법 오류를 정확히 회피(`:118-122`, `:72`).
- **프로필 이미지 교체 시 기존 S3 객체 삭제** + 존재 확인(`UserService.java:134-136`).
  대부분의 코드베이스가 빠뜨리는 부분이다.
- **홈 화면 집계에 사용자 간 유출이 없다.** 알바생은 `findAllByUserId`, 사장님은
  `findAllByOwnerId`로 시작해 요청자 범위를 벗어나지 않는다. 월 범위도 `YearMonth`로
  경계 지어져 무한 범위가 없다. **확인 완료.**
- **`deleteWorkerForOwner`가 사장님의 자기 자신 삭제를 명시적으로 차단**(`:275-277`).
- `WorkerSummaryResponse`가 닉네임·프로필·라벨색만 노출. 급여·연락처 없음.
- **필요한 헬퍼가 이미 리포지토리에 존재한다** (`existsByIdAndWorkplaceId`,
  `findByIdAndWorkplaceId`) — C2 수정은 새 코드가 아니라 기존 헬퍼 재사용이면 끝난다.

---

## Critical

### C1 — 신규 가입자가 스스로 `ROLE_ADMIN`을 부여할 수 있음 ✅ **수정 완료** (`d2a6021`)

```java
// AuthController.java:199-204
.role(Role.valueOf(registerRequest.getRole()))   // ← 검증 없는 사용자 입력 String
```

`RegisterRequest.role`은 `String`(`:19`), `Role` enum은 `ROLE_ADMIN` 포함,
`completeCreateUser:93-108`은 `nickname != null`(가입 완료 여부)만 확인하고
**역할 화이트리스트가 없다.** `/auth/**`는 `permitAll`(`SecurityConfig:45`).

**공격 경로 (전 구간 확인 완료):**
1. 아무 구글 계정으로 `POST /auth/login` → 202 + accessToken
2. `PATCH /auth/login/register` `{"nickname":"홍길동","role":"ROLE_ADMIN"}` → 200
3. 다음 로그인 시 JWT에 `ROLE_ADMIN`. `SecurityConfig:49`가 `/admin/**` 전체 개방
4. `DELETE /admin/users/immediate` → **유예기간 무시하고 삭제 대기 전 유저 즉시 영구 삭제**
   (`AdminService.java:41-48`). 복구 불가
5. `POST /admin/alarms/announcements` → 전 사용자 푸시 발송

**수정**: `completeCreateUser`에 한 줄.
```java
if (role != Role.ROLE_WORKER && role != Role.ROLE_OWNER) throw new InvalidArgumentException();
```
추가로 `RegisterRequest.role`에 `@Pattern(regexp="ROLE_WORKER|ROLE_OWNER")` + `@Valid`.

### C2 — 사장님이 남의 근무지 알바생 급여를 덮어쓸 수 있음 ✅ **수정 완료** (`d2a6021`)

```java
// WorkerService.java:225-233
Long workplaceOwnerId = workplaceRepository.findById(workplaceId)...getOwnerId();
permissionVerifyUtil.verifyOwnerPermission(user.getId(), workplaceOwnerId);   // workplaceId만 검증
...
Long salaryId = salaryRepository.findByWorkerId(workerId)...getId();          // ← workerId 미검증
salaryRepository.update(newSalary);   // WHERE id AND worker_id → 성공
```

**`workerId`가 `workplaceId`에 속하는지 한 번도 확인하지 않는다.** 확인 완료.

**공격**: 사장 O가 근무지 A(id=10) 소유. 피해자 W는 무관한 근무지 B의 `workerId=99`.
`PATCH /workplaces/10/workers/99` →
- `findById(10)` → ownerId=O → 권한 통과
- `findByWorkerId(99)` → W의 salary 행(근무지 스코프 없음) → **update 성공**
- **O가 W의 시급·고정급·4대보험·주휴·야간수당 플래그를 임의 재작성**
- 이어서 `:246-253`이 **W의 `works.estimated_net_income`까지 덮어씀**

`workerId`는 순차 BIGINT라 열거가 자명하다. 근무지 B의 사장님도 W도 인지할 방법이 없다.

**수정**: `:227` 직후 한 줄. 이미 존재하는 헬퍼 재사용.
```java
if (!workerRepository.existsByIdAndWorkplaceId(workerId, workplaceId)) throw new WorkerNotFoundException();
```
(`WorkerService.java:170`의 `getWorkerAttendanceInfo`가 정확히 이 패턴을 이미 쓴다.)

### C3 — `is_accepted`를 읽는 코드가 0개 (확정 정책 4(c)(d) 전면 미구현)

**3번째 독립 확증.** 승인 없이 즉시 가능한 것:
- `POST /workplaces/{id}/workers/me/works` — 근무 등록 성공
- `POST .../works/start` — 출근 처리 성공
- 근무 캘린더 조회 — 근무지 **전 근무자 명단 + 일별 스케줄 + `estimatedNetIncome`** (I5와 결합)

정책 4(d) `PENDING_APPROVAL` 상태 필드도 어떤 응답 DTO에도 없다.

**수정 지점 정정**: 근본 지점은 `PermissionVerifyUtil`이 아니라 **근무 생성 진입점**
(`WorkService.createMyWork`, `updateActualStartTime`)이다. 정책 4가 (a)(b)(d)를
허용하므로 공유 유틸에서 전면 차단하면 안 된다.

### C4 — 확정 정책 3 미착수 · 현재는 데이터 전량 소실

`db/moup.sql:102`의 `workplaces.owner_id ON DELETE CASCADE` +
`UserService:167-169`의 실제 `DELETE FROM users` → 근무지 → 근무자 → 근무·급여 연쇄 삭제.

**C1과 결합하면 아무나 이걸 트리거할 수 있다.**

> **원장 관리자 정정**: 리뷰어는 "정책 3이 스키마상 불가능"이라고 표현했으나 부정확하다.
> 확정 정책 5에서 채택한 방식(**하드 삭제를 가명처리로 대체**)이면 CASCADE가 애초에
> 발화하지 않으므로 **DDL이 불필요하다.** 리뷰어의 지적은 "현재 미착수"로 읽으면 정확하다.

`provider_id` 치환은 재가입 시 `unique_provider` 충돌뿐 아니라 **"부활" 로그인**
(`AuthController:117`)도 막는다 — 확정 정책 5에서 이미 반영됨.

### C5 — 휴게시간 하한 없음 (금액 버그, 3번째 확증)

`WorkersWorkCreateRequest` / `WorkerWorkUpdateRequest`의 `restTimeMinutes`에
`@NotNull`만 있고 하한이 없다.

**산수**: 09:00–17:00 (gross 480분). 정상(rest=60) → net 420 → **70,210원**.
`rest = -600` → net 1080 → **180,540원**. **1회당 +110,330원.**
반복 상한이 365일이라 단일 요청으로 대량 생성 가능하고, 확정 정책 2(스냅샷)에 따라
**사후 정정이 불가능하다.**

---

## Important (요약)

| # | 내용 |
|---|---|
| **I1** ✅ `f5bb991` | **소프트 삭제 유저가 그대로 인증·행위 가능.** `CustomUserDetailsService:29-32`가 `is_deleted` 미검사. `WorkerController`의 7개 엔드포인트가 `getCurrentUserId()`만 쓴다 → 탈퇴 신청자가 유예 3일간 승인/거절·근무자 삭제·근무 생성 수행 후 하드 삭제됨 |
| **I2** | **3일 유예 후 하드 삭제가 자동 실행되지 않음.** `@Scheduled`/`@EnableScheduling`이 코드베이스에 **없다.** `hardDeleteOldUsers`의 유일한 호출자가 수동 API. `deleted_at`이 쌓이기만 함 |
| **I3** ✅ `59fe709` | **사장님의 라벨 색상 수정이 조용히 무동작.** `WorkerRepository:155`의 `WHERE ... AND user_id = #{userId}`에 **사장님 id**가 들어가는데 대상 행의 `user_id`는 **알바생 id** → 절대 매치 안 함. 서버는 204, 앱 재진입 시 원복. 급여는 같은 요청에서 성공하므로 "일부만 반영" |
| **I4** | **`acceptWorker`/`rejectWorker` 멱등성 없음.** 재승인 시 푸시 스팸. **더 심각: 이미 승인된 근무자에게 `DELETE .../accept` 호출 → `workers` 행 삭제 → CASCADE로 그 알바생의 근무·급여 전체 영구 삭제.** "거절" API가 "재직자 이력 전체 삭제"로 동작 |
| **I5** | 알바생이 같은 근무지 동료의 일별 `estimatedNetIncome` 조회 가능 (스코프 2 I1 확증). C3과 결합하면 **미승인자도** 획득 |
| **I6** | `Worker`/`Workplace`/`Salary`/`Work` 4개에 `@NoArgsConstructor` 없음 → MyBatis 위치 기반 생성자 매핑. `User`만 안전. **C4 수정으로 `withdrawn_at` 컬럼을 추가하는 작업 자체가 이걸 터뜨린다** |
| **I7** ✅ `f5bb991` | `UserService:62-63`의 `socialRefreshToken.isEmpty()` NPE. Google이 refresh token을 안 줄 때 **최초 가입 500**. 같은 저자가 기존 유저 분기(`AuthController:121`)에는 null 가드를 넣었다 |
| **I8** | 트랜잭션 내 외부 호출 — `acceptWorker`가 **FCM을 DB 업데이트보다 먼저** 수행, `updateProfileImage`가 **S3 삭제를 업로드보다 먼저** 수행(실패 시 죽은 URL), `updateMyWorker`/`updateWorkerForOwner`/`deleteMyWorker`/`deleteWorkerForOwner`에 **`@Transactional` 없음** |
| **I9** | DTO 검증 vs 스키마 — 라벨색 6곳 `@Size(max=10)` 누락, `memo` `@Size(max=200)` 누락, `LoginRequest.username` 검증·`@Valid` 모두 없음 |
| **I10** | 사장님 홈 "인건비"가 **세후 실지급액**(`netIncome`) 합산. 세전 200만원 기준 실제 사업주 부담 약 219만원 대비 **약 17% 과소 표시**. `grossIncome` 필드가 이미 있는데 안 쓴다 |

---

## Minor (요약)

`NameVerifyUtil.verifyName` 죽은 코드(호출자 0) · 닉네임 8자 제한 vs 스키마 20자 불일치 ·
`getWorkerAttendanceInfo`의 404/403 응답 차이로 소속 열거 가능 ·
`FailedWorkerInfo.reason`에 내부 예외 메시지 노출 · `AdminService.notify`의 미사용 변수 +
`announce`에 `@Transactional` 없음 · 소프트 삭제 시 `fcm_token` 유지(3일간 푸시 수신) ·
**하드 삭제 시 S3 프로필 이미지 미삭제** · `UserDeleteResponse.deletedAt`이 DB
`CURRENT_TIMESTAMP()`와 다른 값 · `UserRestoreResponse` 죽은 DTO ·
**`workers`에 `(user_id, workplace_id)` 인덱스 없음** (UNIQUE를 걸면 인덱스와 중복 참여
방지를 동시에 얻음 — 스코프 5 C-2와 동일 결론)

---

## 확정 정책 위반 여부

| 정책 | 판정 | 근거 |
|---|---|---|
| **1. 사장님의 알바생 근무 조회·수정·삭제** | **위반** | `PermissionVerifyUtil:10` NPE → 500 (탈퇴 알바생 대상). 정상 알바생에는 정상 동작 |
| **2. 급여 스냅샷 유지** | **부분 준수 — 확인 필요** | `hourly_rate`·`base_pay`·`night_allowance`·`holiday_allowance`·`gross_income`은 손대지 않음 — **정확**. 다만 `WorkerService:216`/`:247`이 **당월 전체(지난 날짜 포함)의 `estimated_net_income`을 덮어씀** → Q1 |
| **3. 사장님 탈퇴 시 데이터 보존** | **위반 — 미착수** | C4 |
| **4. 승인 대기 가시 범위** | **(a)(b) 준수 / (c)(d) 위반** | C3 |

---

## 확인 질문

| # | 질문 | 블로킹 |
|---|---|---|
| **Q1** | 급여 설정 변경 시 **당월 이미 지난 근무일의 `estimated_net_income`**이 새 공제 기준으로 갱신되는 것이 의도인가? 월 단위 공제를 일별 N등분하는 구조상 갱신하지 않으면 당월 합계가 안 맞는다 | 확정 정책 2 해석 |
| ~~**Q2**~~ | **답변 완료 → [확정 정책 16](../review/README.md#확정-정책-색인). 재로그인 전까지 전면 차단.** 탈퇴 신청 즉시 401이며, 유예기간 3일 내 소셜 재로그인 시 자동 복구된다. `f5bb991`의 C2 수정이 이 동작이다 | 해소 |
| **Q3** | 사장님 홈 "인건비"에 사업주 부담 4대보험을 포함할 것인가? (I10) | I10 수정 방향 |
| ~~**Q4**~~ | **오탐 — 해제는 정상 동작한다.** `updateActualEndTime`이 `workerRepository.updateIsNowWorking(..., false)`를 직접 호출하고(`WorkService:692`), 진행 중 근무가 없는데 플래그만 true인 비정상 상태를 복구하는 분기까지 있다(`:695`). 서비스 래퍼 `updateWorkerIsNowWorking`의 호출자만 찾아 실제 경로를 놓친 것이다. 그 래퍼는 호출자가 0건이라 `c8109f4`에서 제거했다 | 해소 |
| ~~**Q5**~~ | **누락이었다.** [Phase 1](fix-plan.md#-phase-1--완료)에서 `receiver_id` CASCADE · `sender_id` SET NULL FK를 추가했다 | 해소 |

**역할 전환은 불가능함이 확인됐다** — `role` 변경 경로는 `UserRepository.updateById` 하나뿐이고
호출자는 `completeCreateUser`(닉네임 null일 때만)뿐. 스코프 5 Q4는 **해소**.

---

## 테스트 우선순위

이 스코프의 커버리지는 사실상 **0**이다. `UserService`, `HomeService`, `AdminService`,
`UserDeletionService` **전체**와 `WorkerService`의 `acceptWorker` 외 10개 메서드.

1. **C1 회귀** — `completeCreateUser`에 `ROLE_ADMIN` 전달 시 예외
2. **C2 회귀** — `updateWorkerForOwner(owner, workplaceA, workerOfB)` → 예외 +
   `verify(salaryRepository, never()).update(any())`
3. **C5 산수** — `restTimeMinutes = -600` → 400, 그리고 `basePay`가 gross 기준을 넘지 않음
4. **탈퇴 신호** — `user_id IS NULL`인 `Worker`에 대해 `deleteWorkerForOwner`,
   `verifyWorkerPermission`, `getWorkerList`가 NPE 없이 정의된 동작.
   **가명처리 전환 전에 작성해야** 전환 후 무엇이 깨지는지 알려준다
5. **C3 게이트** — `is_accepted=false`의 `createMyWork`/출근이 403
6. **I3** — `updateWorkerForOwner` 후 `owner_based_label_color`가 실제로 바뀌었는지
   (현재 무동작이므로 지금 작성하면 즉시 실패)

---

## 총평

잘한 부분과 못한 부분이 뚜렷하게 갈린다. N+1 방지, 빈 컬렉션 처리, S3 객체 정리,
홈 집계의 사용자 스코핑, 역할 게이트 문법 — 이 규모에서 자주 놓치는 것들이 제대로 되어 있다.

문제는 **경계**다. 권한 검사가 "이 사장님이 이 근무지 주인인가"에서 멈추고 "이 근무자가
이 근무지 소속인가"로 이어지지 않는다(C2). 역할 할당이 사용자 입력을 그대로 신뢰한다(C1).
`is_accepted` 컬럼을 만들고 값을 넣고 갱신 API까지 만들어놓고 **한 번도 읽지 않는다**(C3).

탈퇴 신호 조사의 가장 중요한 발견은 **"이미 깨져 있다"**는 것이다. 가명처리 전환은 이
문제를 바꾸는 게 아니라 **다른 방향으로 뒤집는다** — 지금은 탈퇴자가 안 보이고, 나중에는
살아있는 사람으로 보인다. 두 상태 모두 틀렸다. **`workers.withdrawn_at` 컬럼 하나가
이 전체를 해결한다.**

수정 규모는 생각보다 작다. C1은 조건문 한 줄, C2는 기존 헬퍼 한 줄, C5는 애노테이션 하나,
I3은 WHERE에서 조건 하나 제거, I6은 애노테이션 네 개, I7은 null 검사 하나.
**C3과 C4만 실제 설계 작업이다.**
