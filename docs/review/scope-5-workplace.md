# 스코프 5 — 근무지(Workplace) · 초대코드(InviteCode) 도메인

- **범위**: `server/src/main/java/com/moup/domain/workplace/` 전체 ~1,460 LOC
- **판정**: **수정 후**
- **집계**: Critical 3 / Important 9 / Minor 9 / 확인 질문 5
- **리뷰 격리**: `docs/review/` 차단, 확정 정책 2건만 전제로 제공
- **외부 호출자**: `WorkplaceService` / `InviteCodeService`의 패키지 외부 호출자 **0건**
  — 이 서브시스템은 자기완결적이다

---

## 교차 검증 — 이전 스코프와의 대조

| 스코프 5 | 이전 | 판정 |
|---|---|---|
| **C-1** `is_accepted`가 어디서도 읽히지 않음 | 스코프 2 **I2** | **독립 확증 + 대폭 확장.** 스코프 2는 "근무 도메인이 검사하지 않는다"까지였는데, 스코프 5는 **전체 코드베이스에 `SELECT`/`WHERE`/조건문이 0건**임을 확인하고 스키마 결함(`NULL` 허용, DEFAULT 없음)까지 짚었다. 심각도 Important → **Critical** |
| I-3 요청 DTO 검증 누락 | 스코프 3 I-3/I-4 계열 | 같은 패턴이 근무지 DTO에도 존재 |
| I-6 트랜잭션 안 외부 호출 | 스코프 2 C5 계열 | 다른 지점, 같은 결함 유형 |

### 자체 검증 (원장 관리자)

`is_accepted` 전수 grep 결과 — INSERT 컬럼 목록 1, `UPDATE ... SET` 1, DTO 빌더 3,
필드 선언 1, 테스트 4. **읽는 곳 0건.** C-1 확정.
`workers` 스키마에 `PRIMARY KEY (id)` + FK 2개가 전부. **UNIQUE 없음.** C-2 확정.

---

## 잘 된 점

- **초대코드 저장소 설계가 견고하다.** Redis 양방향 매핑(`inviteCode:` ↔ `workplaceId:`)을
  `SessionCallback` + `MULTI/EXEC`로 묶어 한쪽만 남는 상태를 막았고
  (`InviteCodeRepository.java:32-48`), 양쪽에 동일한 10분 TTL을 걸었다.
- **알파벳 선택이 좋다.** `"23456789ABCDEFGHJKLMNPQRSTUVWXYZ"` — `0/O/1/I`를 뺀 32자
  (`InviteCodeService.java:21`). 구두 전달을 고려한 판단이고, 32는 2의 거듭제곱이라
  modulo bias도 없다.
- **Redis 장애 시 fail-closed.** `RedisConnectionFailureException`이
  `GlobalExceptionHandler.java:113`으로 떨어져 500이 나간다. 초대 게이트가 열린 채
  통과되지 않는다. (명시적 의도는 아니나 결과가 맞다.)
- **N+1이 실제로 제거돼 있다.** `getAllWorkplace`(`WorkplaceService.java:171-198`)가
  `findAllByUserId` → `findAllByIdListIn` 2쿼리로 끝내고 필터/정렬을 메모리에서 한다.
- **동적 SQL이 안전하다.** `${}` 보간 0건. `foreach` 두 곳 모두 호출부에서 빈 리스트를
  사전 차단(`WorkplaceService.java:181-183`).
- **`NameVerifyUtil`을 근무지 이름에 적용하지 않은 것이 옳다.** 이 유틸은 8자·한영 혼용
  불가 닉네임 규칙이라 "세븐일레븐 동탄중심상가점"이 걸린다. 미적용이 정답 — 지적 사항 아님.
- **발급 권한이 이중으로 검증된다.** `@PreAuthorize(ROLE_OWNER)`(`WorkplaceController.java:122`)
  + `verifyOwnerPermission`(`WorkplaceService.java:304`). 남의 근무지 코드 발급 불가.
- **조회 응답이 과다 노출하지 않는다.** `InviteCodeInquiryResponse`에 사장님 신원·근무자
  명단·급여가 없다.
- 초대코드 재시도가 `maxAttempts = 10`으로 유계(`InviteCodeService.java:46`).

---

## Critical

### C-1 — `is_accepted` 승인 게이트가 어디서도 읽히지 않는다 (승인 절차가 장식)

참여자는 `.isAccepted(false)`로 생성되고(`WorkplaceJoinRequest.java:35`),
`WorkerService.java:296`의 `acceptWorker`가 `true`로 갱신한다. 여기까지는 정상.

**문제: 전체 코드베이스에서 `is_accepted`를 `WHERE`절이나 조건문으로 읽는 곳이 0건이다.**
출현 위치 전부 — INSERT 컬럼 1, `UPDATE ... SET` 1, DTO 빌더 3, 필드 선언 1, 테스트 4.
`SELECT`도 `if`도 없다.

결과적으로 미승인 상태에서:
- `WorkplaceService.getWorkplaceDetail`(`:110`) — `findByUserIdAndWorkplaceId(...).orElseThrow()`만
  통과하면 근무지 이름·주소·GPS 좌표 반환. **행이 존재하기만 하면 된다.**
- `WorkplaceService.getWorkplace`(`:159`)의 `existsByUserIdAndWorkplaceId`도 동일
- 스코프 밖 같은 뿌리: `WorkService.java:102-106, 405, 582, 606`도 전부
  `findByUserIdAndWorkplaceId` + `verifyWorkerPermission`이며,
  `PermissionVerifyUtil.java:8-13`은 `is_accepted`를 인자로 받지도 않는다

**실패 시나리오**: 사장님의 "승인" 버튼이 아무것도 막지 않는다. 초대코드를 손에 넣은 사람은
`POST /workplaces/join` 201을 받는 순간 이미 승인된 근무자와 동일한 권한을 갖는다.
`rejectWorker` 전까지의 시간 창이 아니라, **승인/거절이라는 개념 자체가 read path에 없다.**

**수정**: 모든 경로가 이미 `PermissionVerifyUtil`을 지나가므로 호출부마다 막지 말고
거기서 한 번 막는다.

```java
public void verifyWorkerPermission(Long requesterUserId, Worker worker, Long workplaceOwnerId) {
    if (workplaceOwnerId.equals(requesterUserId)) return;              // 사장님 통과 (정책 1)
    if (!worker.getUserId().equals(requesterUserId)) throw new InvalidPermissionAccessException();
    if (!Boolean.TRUE.equals(worker.getIsAccepted())) throw new InvalidPermissionAccessException();
}
```

`WorkplaceService.getWorkplaceDetail:110` / `getWorkplace:159`도 이 경로를 타게 한다.
**어떤 화면이 "승인 대기" 상태에서 보여야 하는지는 정책 미정의 → Q1.**

**부수 스키마 결함**: `workers.is_accepted TINYINT(1) NULL`(`db/moup.sql:112`) —
DEFAULT도 NOT NULL도 없어 앱을 우회한 행은 `NULL`(승인도 미승인도 아님)이 된다.
`NOT NULL DEFAULT 0`이 맞다.

### C-2 — 중복 참여를 막는 것이 check-then-insert뿐 · 성공 시 해당 사용자의 근무지가 영구 500

`WorkplaceService.java:341-348`
```java
if (workerRepository.existsByUserIdAndWorkplaceId(user.getId(), workplaceId)) { throw new WorkerAlreadyExistsException(); }
...
workerRepository.create(worker);
```

`workers`에 `(user_id, workplace_id)` UNIQUE가 **없다**(확인함: PK + FK 2개가 전부).

**실패 시나리오 (공격보다 사고가 훨씬 잦다)**: 모바일에서 "참여"를 빠르게 두 번 탭하거나
느린 네트워크에서 요청이 재시도되면 두 요청이 동시에 들어온다. `REPEATABLE READ`에서
둘 다 `false`를 읽고 둘 다 INSERT — 막을 제약이 없으므로 **둘 다 성공.**
`workers` 2행 + `salaries` 2행.

그 다음이 진짜 피해다. `WorkerRepository.java:81-82`:
```java
@Select("SELECT * FROM workers WHERE user_id = #{userId} AND workplace_id = #{workplaceId}")
Optional<Worker> findByUserIdAndWorkplaceId(Long userId, Long workplaceId);
```
MyBatis `selectOne`은 2행이면 `TooManyResultsException`(RuntimeException) → 
`GlobalExceptionHandler:113` → **500**. 이 메서드는 `getWorkplaceDetail:110`,
`updateWorkplaceAndWorkerHelper:294`, `deleteWorkplace:258`, `WorkService`의 근무
등록·조회 경로 전부가 쓴다.

**→ 더블탭 한 번으로 그 사용자는 해당 근무지의 상세 조회·수정·탈퇴·근무 등록이 전부
영구 500이 되고, 탈퇴조차 안 되므로 스스로 복구할 수 없다.** DBA가 손으로 행을 지워야 한다.
이 리뷰에서 실제로 터질 확률이 가장 높은 버그다.

**수정**:
```sql
ALTER TABLE workers ADD UNIQUE KEY unique_user_workplace (user_id, workplace_id);
```
`user_id`가 NULL 허용이므로 탈퇴 사용자의 NULL 행 여러 개는 MySQL UNIQUE 규칙상
충돌하지 않는다 — 기존 "탈퇴한 근무자" 보존 설계와 그대로 양립한다.
`joinWorkplace`에서 `DuplicateKeyException`을 `WorkerAlreadyExistsException`(409)로 변환하고
기존 exists 체크는 빠른 경로로 남긴다.

**마이그레이션 주의** — 제약 추가 전 기존 중복 정리 필요:
```sql
SELECT user_id, workplace_id, COUNT(*) FROM workers WHERE user_id IS NOT NULL
GROUP BY user_id, workplace_id HAVING COUNT(*) > 1;
```

### C-3 — 초대코드 상환에 레이트 리밋이 전무 · 무표적 공격이 현실적이고 C-1과 연쇄

`ratelimit|bucket4j|resilience4j|attempt` 전체 grep 0건. `SecurityConfig`에도 없다.

`GET /workplaces/invite-codes/{inviteCode}`가 가장 싼 오라클이다 — body 없음,
200(존재)/404(부재)로 즉시 갈린다. 필요한 건 무료 소셜 로그인 JWT 하나.

**표적 공격은 안전하다.** 키스페이스 32⁶ = 1,073,741,824(2³⁰), TTL 600초.
한 수명 안 적중 확률 ≈ 600R / 2³⁰:

| 시도율 | 한 TTL 창 적중 확률 |
|---|---|
| 100 rps | 0.0056% |
| 2,000 rps | 0.11% |
| 50% 도달에 필요한 속도 | **약 124만 rps** — 비현실적 |

**무표적 공격이 진짜다.** 동시 유효 코드 L개면 첫 성공까지 기댓값 2³⁰/L회:

| L (동시 유효 코드) | 2,000 rps 기준 소요 |
|---|---|
| 50 | 약 **3시간** |
| 500 | 약 **18분** |
| 5,000 | 약 **1.8분** |

**즉 제품이 성장할수록 나빠진다.** 그리고 뚫린 뒤 C-1 때문에 승인 없이 완전한 근무자
권한을 얻는다. 200/404 오라클만으로도 근무지 이름 + 주소 + GPS가 새어나가 스크래핑
벡터가 된다. 초당 2,000회의 404 자체가 Redis·JWT 필터에 대한 DoS이기도 하다.

**수정**: Redis가 이미 있으니 새 의존성 없이 몇 줄.
```java
public long recordAttempt(Long userId) {
    String key = "inviteAttempt:" + userId;
    Long n = stringRedisTemplate.opsForValue().increment(key);
    if (n != null && n == 1L) stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
    return n == null ? 0 : n;
}
```
10분 20회면 정상 사용자는 안 걸리고 위 표가 100배 나빠진다(L=500: 18분 → 30시간).
**실패한 시도만 셀 것.** IP 단위는 nginx/ALB에 두는 게 맞다.
`ErrorCode`에 429가 없으므로 `TOO_MANY_REQUESTS` 추가 필요.

---

## Important

### I-1 — `PATCH`가 전체 치환으로 동작해 주소·좌표를 말없이 지운다 + 업데이트 DTO에 검증 없음

**(a)** `BaseWorkplaceCreateRequest:21,24`에는 `@NotBlank`가 있는데
`BaseWorkplaceUpdateRequest`에는 **Bean Validation이 하나도 없다.** Swagger는
`requiredMode = REQUIRED`라고 선언해 놓고서. `workplaceName: null` →
`UPDATE ... SET workplace_name = NULL` → NOT NULL 위반 → **500**(422여야 함).

**(b) 더 나쁜 쪽**: `@PatchMapping`인데 SQL(`WorkplaceRepository.java:76-81`)이 전 컬럼을
무조건 덮어쓴다. 클라이언트가 PATCH 의미대로 일부 필드만 보내면 `address`,
`latitude`, `longitude`가 전부 `NULL`이 된다. **말없이 데이터가 지워지고 204가 돌아간다.**
지도에 찍히던 근무지가 사라지고 클라이언트는 성공했다고 믿는다.

**수정 (택1)**: ① Swagger 계약대로 `@NotBlank` + `@PutMapping`(전체 치환 명시) —
diff가 짧고 현재 동작과 일치. ② MyBatis `<set>` + `<if>`로 진짜 PATCH.
**클라이언트가 부분 수정을 실제로 쓰는지 확인 필요 → Q3.**

### I-2 — 근무지 수정에 소유권 검증 없음 · 0행 갱신 후 204 반환 (거짓 성공)

`updateWorkplaceAndWorkerHelper`(`WorkplaceService.java:282-296`)가
`verifyOwnerPermission`을 호출하지 않는다. `generateInviteCode:304`는 제대로 하는데
여기만 빠졌다.

**데이터 무결성은 지켜진다** — `WHERE id = #{id} AND owner_id = #{ownerId}`가 막는다.
문제는 `update`가 `void`라 영향 행 수를 안 본다는 것. 공유 근무지 알바생이 이름을
바꾸면 0행 갱신 + 예외 없음 → 라벨색·급여는 정상 수행 → **204 No Content**.
클라이언트는 바뀐 줄 알고 로컬 상태를 갱신하고, 다음 조회에서 옛 이름이 돌아와 앱 상태가 튄다.

**수정**: `permissionVerifyUtil.verifyOwnerPermission(userId, oldWorkplace.getOwnerId());` 한 줄.
단 공유 근무지 알바생이 자기 급여/라벨색을 고치는 정상 흐름을 막으므로, 알바생 경로는
근무지 정보 갱신을 건너뛰고 급여/라벨만 갱신하도록 분기할 것 — 어차피 지금도 0행이므로
동작 변화 없이 응답만 정직해진다.

### I-3 — 요청 DTO 검증이 스키마 제약을 반영하지 않음 (422여야 할 것이 500)

| 필드 | 스키마 | DTO 검증 | 초과 시 |
|---|---|---|---|
| `workplaceName` | `VARCHAR(50) NOT NULL` | `@NotBlank`만 | 51자 → **500** |
| `categoryName` | `VARCHAR(10) NOT NULL` | `@NotBlank`만 | 11자 → **500** |
| `address` | `VARCHAR(100)` | 없음 | 101자 → **500** |
| `workerBasedLabelColor` | `VARCHAR(10)` | `@NotBlank`만 | 11자 → **500** |
| `latitude`/`longitude` | `DECIMAL(9,6)` | 없음 | `99999` 통과 → **500** |

`categoryName VARCHAR(10)`이 특히 위험 — 한글 10자면 "24시간무인편의점"에서 이미 한계다.
`latitude` 유효 범위는 [-90, 90]인데 검증이 아예 없다.

`GlobalExceptionHandler:71-85`가 이미 `MethodArgumentNotValidException`을 필드명까지 담은
422로 잘 변환하므로 **애노테이션만 붙이면 그 경로를 탄다.**

### I-4 — 초대코드 난수원이 CSPRNG가 아니다 (`ThreadLocalRandom`)

`InviteCodeService.java:22-24`가 `.usingRandom(...)`을 호출하지 않는다.
리뷰어가 commons-text **1.14.0** jar를 `unzip` + `javap -c`로 열어 바이트코드에서 확인:

```
private int generateRandomNumber(int, int);
   1: getfield #41  // Field random:Ljava/util/function/IntUnaryOperator;
   4: ifnull  24
  24: invokestatic #144 // java/util/concurrent/ThreadLocalRandom.current()
```

`random` 필드가 null이면 `ThreadLocalRandom`으로 폴백한다. 추측이 아니라 바이트코드다.

**정직한 평가**: 코드 6자는 관측 가능 정보가 30비트뿐이라 단일 코드로 64비트 상태 복원은
2³⁴ 규모 탐색이 필요하다. **원클릭 익스플로잇은 아니다.** 다만 정당한 사장님이
`forceGenerate:true`를 **횟수 제한 없이**(C-3) 호출해 같은 스레드 풀의 RNG 체인에서
표본을 무한 수집하면 상태 복원은 tractable해지고, 성공 시 그 스레드가 생성하는
**모든 근무지의 코드**를 예측한다.

**수정**: 한 줄. 논쟁할 이유가 없다.
```java
.usingRandom(SECURE_RANDOM::nextInt)
```

### I-5 — 코드 발급이 check-then-set (비원자적) · 충돌 시 기존 매핑을 말없이 덮어씀

`InviteCodeService.java:51-54`의 `EXISTS` → `SET` 사이가 비어 있다. `save`의 `MULTI`는
**두 키의 원자성**을 보장할 뿐 조건부 쓰기가 아니다.

**확률은 낮다**(동시 발급 쌍당 ≈ 9.3×10⁻¹⁰). **하지만 결과가 조용하고 나쁘다** —
사장님 A의 코드가 근무지 B를 가리키게 되어, A가 배포한 코드로 참여하면 **엉뚱한 근무지에
들어간다.** 아무 로그도 없고 A는 왜 알바생이 안 들어오는지 영원히 모른다.

**같은 뿌리의 두 번째 문제**: `forceGenerate` 재발급(`:36-43`)도 `delete` → `save`가
비원자적. 동시 재발급 2건이면 **옛 코드가 살아남아** "코드 폐기"가 실제로 폐기하지 못한다.

**수정**: Redis에 이미 조건부 쓰기가 있다.
```java
Boolean ok = stringRedisTemplate.opsForValue()
        .setIfAbsent(INVITE_CODE_KEY_PREFIX + inviteCode, workplaceId.toString(), 10, TimeUnit.MINUTES);
```
`existsByInviteCode`를 삭제하고 반환값으로 재시도 루프를 돌리면 Redis 왕복도 2 → 1회.
재발급은 Lua 스크립트로 묶거나 근무지 단위 락으로 감쌀 것.

### I-6 — 참여 트랜잭션 안에서 FCM 외부 호출 · Firebase 장애가 참여 전체를 막음

`joinWorkplace`(`WorkplaceService.java:337-376`)가 `@Transactional` 안에서
`workerRepository.create` → `salaryRepository.create` → `fcmService.sendToSingleUser`를
순차 실행한다. FCM 실패가 `CustomFirebaseMessagingException`(RuntimeException)로
재던져져 **롤백이 실제로 일어난다.**

① 푸시 알림 실패는 참여 실패가 아니다. 사장님은 앱을 열면 대기 목록에서 보게 되고
(알림 이력은 DB에 먼저 저장됨), 푸시는 부가 채널이다. ② DB 커넥션을 붙잡은 채
Firebase 왕복을 기다려 참여가 몰리면 풀이 외부 지연에 인질로 잡힌다.

**수정**: `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 커밋 이후로 밀거나,
최소한 `catch (FirebaseMessagingException e) { log.warn(...); }`로 삼킨다.

⚠️ 사라진 테스트 `joinWorkplace_Fail_FCMSendError`가 정확히 이 롤백을 검증하고 있었다.
동작을 바꾸면 그 테스트도 함께 고쳐야 한다.

### I-7 — 근무지 이름 중복·개수 제한도 check-then-insert, DB 제약 없음

`WorkplaceService.java:265-271`. `workplaces`에 `(owner_id, workplace_name)` UNIQUE가 없다.
C-2보다 피해는 가볍다 — `EXISTS`라 `TooManyResults`로 터지지 않고 `findById`는 PK 조회다.

**수정**: C-2와 같은 마이그레이션에서
`ALTER TABLE workplaces ADD UNIQUE KEY unique_owner_workplace_name (owner_id, workplace_name);`
개수 제한은 DB로 표현 불가하므로 코드에 남긴다(20개 초과 몇 개는 실질 피해 없음).

### I-8 — [스키마] `workplaces.owner_id ON DELETE CASCADE` · 사장님 탈퇴 시 전 알바생 이력 소실

`db/moup.sql:100` vs `:114` — 두 FK의 의도가 어긋나 있다:
```sql
-- workplaces
FOREIGN KEY (`owner_id`) REFERENCES users (`id`) ON DELETE CASCADE   -- ← CASCADE
-- workers
FOREIGN KEY (`user_id`)  REFERENCES users (`id`) ON DELETE SET NULL  -- ← SET NULL
```

`UserService.deleteUserSoftlyByUserId`가 3일 유예 후 하드 삭제(`UserRepository.java:52`)하므로
CASCADE가 실제로 발화한다: `users` → `workplaces` → `workers` 전원 → `works`, `salaries` 전원.

**사장님 한 명이 탈퇴하면 그 매장 알바생 전원의 근무 기록과 급여 이력이 통째로 사라진다.**
알바생 본인 탈퇴는 `user_id`만 NULL이 되고 이력이 보존되는데(그래서 "탈퇴한 근무자"
폴백이 있다), 사장님 탈퇴가 그 보존 장치를 무의미하게 만든다.

**구현이 아니라 스키마·정책 문제다.** `owner_id`가 nullable로 선언된 것 자체가 원래
의도는 `SET NULL`이었음을 시사한다. 임금 기록은 근로기준법상 보존 의무 대상이므로
법무 확인도 필요해 보인다 → **Q2.**

⚠️ FK만 바꾸면 즉시 깨지는 곳: `WorkplaceService.java:253`,
`PermissionVerifyUtil.java:17`, `WorkplaceService.java:343, 368`. **정책을 먼저 정할 것** —
지금 FK만 바꾸면 CASCADE 데이터 소실이 NPE 500으로 바뀔 뿐이다.

### I-9 — `SELECT *` + 무인자 생성자 부재 → 물리 컬럼 순서에 매핑이 결합

`Workplace`(`:5-17`)는 `@Getter @Builder @ToString`만 있다 — **무인자 생성자도 setter도
없다.** MyBatis는 기본 생성자가 없으면 `createByConstructorSignature`로 떨어져
**생성자 파라미터를 결과셋 컬럼에 순서대로 대응**시킨다.

현재는 우연히 맞지만, `SELECT *`이므로 컬럼을 중간에 하나 추가하면 타입이 우연히 맞는
지점에서 **예외 없이 값이 어긋난 채 매핑된다.** 주소 컬럼에 사업자번호가 들어가는 식.
**500보다 나쁘다 — 조용하기 때문이다.** 같은 패턴이 `WorkerRepository`의 `SELECT *` 8개
전부에 있다.

**수정**: 컬럼을 명시한다. 한 번 쓰면 순서 결합이 사라지고
`map-underscore-to-camel-case`가 이름으로 매칭한다.

---

## Minor

| # | 내용 |
|---|---|
| M-1 | `InviteCodeService.java:58`의 `throw new RuntimeException("초대 코드 생성에 실패...")` — `GlobalExceptionHandler:113`이 `"서버에 오류가 발생했습니다."`로 뭉개 메시지가 도달하지 않는다. `ErrorCode` + `CustomException` 서브클래스로. |
| M-2 | `WorkplaceService.java:306-309` `returnAlreadyExists` TOCTOU + Redis 왕복 낭비. 두 호출 사이 TTL이 만료되면 `returnAlreadyExists=true`인데 새 코드가 200으로 나간다(201이어야 함). |
| M-3 | TTL 10분이 `InviteCodeRepository.java:41,43`에 하드코딩. `workplace.creation.limit`은 설정으로 뺐는데 이건 안 뺐다. **보안 파라미터**이므로 설정으로. |
| M-4 | `BaseWorkplaceDetailResponse.java:11`에 `@JsonSubTypes`만 있고 `@JsonTypeInfo`가 없다 — 효과 없는 죽은 애노테이션. 형제 클래스들은 제대로 붙였다. 삭제. |
| M-5 | 삭제된 근무지의 초대코드가 Redis에 최대 10분 잔존. `deleteWorkplace:250-262`가 `inviteCodeRepository.delete`를 호출하지 않는다. 하위에서 다 막히고 `id`가 AUTO_INCREMENT라 오염은 없으나 깔끔하지 않다. |
| M-6 | Redis 전용 메서드에 `@Transactional`(`:300`, `:317`). Redis 쓰기는 DB 트랜잭션과 함께 롤백되지 않으므로 보호해 준다는 착각만 준다. |
| M-7 | Swagger 계약 누락 — `generateInviteCode`에 422 없음, `deleteWorkplace`(`:225-228`)에 403·400 없음, I-1의 `requiredMode` 불일치. |
| M-8 | `getWorkplace`(`:141`)의 `@Positive` 위반 시 `GlobalExceptionHandler:107`이 예외 메시지를 그대로 노출해 **내부 메서드·파라미터 이름이 샌다** (`getWorkplace.workplaceId: ...`). |
| M-9 | 초대코드 정규식(`^[a-zA-Z0-9]{6}$`)과 실제 알파벳 불일치. 생성에서 뺀 `0/O/1/I`가 통과해 404가 된다. `MUP2E4`를 `MUPZE4`로 잘못 읽으면 안내 없이 "근무지 없음"만 본다. `O→0` 관용 매핑 또는 정규식을 32자 집합으로 좁힐 것. |
| M-10 | 참여 근무지 개수 무제한. `workplace.creation.limit=20`은 `owner_id` 기준 생성만 센다. `getAllWorkplace`(`:171`)에 페이지네이션도 없다. |

---

## 초대코드 보안 평가

| 항목 | 값 | 판정 |
|---|---|---|
| 알파벳 | 32자 (`0/O/1/I` 제외) | 양호 — bias 없음 |
| 길이 / 키스페이스 | 6 / 32⁶ = 1,073,741,824 (2³⁰) | TTL 결합 시 충분 |
| 엔트로피 | 30비트 | 10분 수명 전제로 적정 |
| **난수원** | `ThreadLocalRandom` (바이트코드 검증) | **미흡 (I-4)** |
| 충돌 검사 | 있음, 최대 10회 유계 | 있으나 **경쟁 상태 (I-5)** |
| 원자성 | `EXISTS` → `SET` (SETNX 아님) | **미흡 (I-5)** |
| **TTL** | **600초**, 양방향 동일, `MULTI` 원자 설정 | **매우 좋음 — 핵심 방어선** |
| 사용 후 무효화 | 없음 (TTL까지 재사용) | 다인 초대 목적상 **의도된 설계로 수용** |
| 퇴사자 코드 보유 | 최대 10분 | 수용 가능 |
| 발급 권한 | `@PreAuthorize` + `verifyOwnerPermission` **이중** | **양호** |
| 조회 응답 노출 | 사장님 신원·명단·급여 **없음** | **양호** |
| **레이트 리밋** | **전무** | **미흡 (C-3)** |
| Redis 장애 시 | fail-closed (500) | **양호** |
| 키 충돌 / 직렬화 | 접두사 분리 / `StringRedisTemplate` 평문 | 양호 — 역직렬화 가젯 위험 없음 |

### 종합 판정: 조건부 안전 — 뼈대는 옳고, 두 곳이 비어 있다

**안전한 부분**: 32⁶ 키스페이스 + 10분 TTL 조합은 **표적 공격에 안전하다.** 한 수명 안
50% 적중에 초당 124만 요청이 필요하다. **키스페이스를 늘릴 필요 없다** — 6자리는
사용성과 보안의 합리적 균형점이고 TTL이 나머지를 감당한다. 발급 권한 이중 검증과
조회 응답 비노출도 확인됐다.

**안전하지 않은 부분**: 무표적 공격. L=500이면 약 18분에 *어느* 근무지엔가 들어가고,
들어간 순간 C-1 때문에 승인 없이 완전한 권한을 얻는다. **이 둘의 연쇄가 실제 유출
경로를 만든다.**

**한 문장**: 키스페이스도 TTL도 문제가 아니다. **레이트 리밋(C-3)과 승인 게이트(C-1)
두 개만 채우면 이 초대 게이트는 건강하다.**

---

## 확인 질문

| # | 질문 | 블로킹 |
|---|---|---|
| **Q1** | 승인 대기(`is_accepted = false`) 상태에서 알바생이 무엇을 볼 수 있어야 하는가? (a) 근무지 이름/주소 (b) 자기 급여 설정 (c) 자기 근무 등록 (d) 근무지 목록 표시. 리뷰어 의견은 (a)(d)는 "대기 중" 배지와 함께 허용, (b)(c)는 차단 — **추측이므로 미구현** | **C-1 수정 방향** |
| **Q2** | 사장님 탈퇴 시 그 매장과 알바생들의 근무·급여 이력은? 현재 스키마는 "전부 삭제", 알바생 탈퇴는 보존 — 두 정책이 모순. 근로기준법 임금대장 보존 의무(3년) 관련 법무 확인 필요 | **I-8 수정 방향** |
| **Q3** | 클라이언트가 `PATCH /workplaces/{id}`를 부분 갱신으로 쓰고 있는가? | **I-1 수정 방향** |
| **Q4** | 사용자 역할이 `ROLE_WORKER` → `ROLE_OWNER`로 변경될 수 있는가? 가능하면 알바생 시절 만든 개인 근무지(`is_shared=false`)가 초대코드 발급 대상이 되는데 `is_shared`가 갱신되지 않는다. 역할 변경 API가 없다면 무시 가능 | — |
| **Q5** | `worker_based_label_color` / `ownerBasedLabelColor`가 고정 집합인가? enum이면 `@Pattern`, 자유 입력이면 최소 `@Size(max=10)` | I-3 |

---

## 테스트 우선순위

**초대 게이트에 대한 테스트는 기존 테스트가 초록색이던 시절에도 단 하나도 없었다.**
생성·조회·발급 권한·충돌·TTL 어느 것도 검증된 적이 없다. 이 서브시스템에서 가장
중요한 것이 가장 검증이 안 되어 있다.

사라진 커버리지(현재 실패 중): `createWorkplace_Fail_LimitExceeded`,
`createWorkplace_Success_UnderLimit`, `joinWorkplace_Success_FCMSend`,
`joinWorkplace_Fail_FCMSendError`(= I-6이 건드리는 바로 그 동작).

| # | 대상 | 입력 | 기대 | 현재 |
|---|---|---|---|---|
| 1 | **미승인 근무자 차단 (C-1)** | 유효 코드로 참여 후 `is_accepted=false` 상태에서 `GET /workplaces/{W}?view=detail` | **403** `PERMISSION_403`, 승인 후 200 | **200 + 이름·주소·GPS** ❌ |
| 2 | **중복 참여 차단 (C-2)** | 같은 코드로 `POST /workplaces/join` **동시 2회**(`CountDownLatch`) | 201 + **409**, `COUNT(*)` = 1 | 둘 다 201, 행 2개, 이후 조회 **500** ❌ |
| 3 | 난수원 CSPRNG (I-4) | `.usingRandom(...)` 호출 여부를 리플렉션/생성자 주입으로 검증 | `SecureRandom` | `ThreadLocalRandom` ❌ |
| 4 | `forceGenerate` 옛 코드 무효화 (I-5) | C1 발급 → 재발급 C2 → C1으로 참여 | **404**, C2는 201. 동시 재발급 시 **정확히 하나만** 유효 | 동시 시 둘 다 유효 ❌ |
| 5 | 상환 레이트 리밋 (C-3) | 같은 JWT로 없는 코드 21회 조회 | 20회 404, **21번째 429** | 21회 전부 404 ❌ |
| 6 | PATCH 필드 보존 (I-1) | `address`/`latitude` 있는 근무지에 이름만 PATCH | 둘 다 **보존** | 둘 다 `NULL` ❌ |
| 7 | 비소유자 수정 (I-2) | 알바생이 `PATCH /workplaces/{W}` 이름 변경 | **403** | **204** (데이터는 안전, 응답이 거짓) ❌ |
| 8 | 스키마 경계값 (I-3) | 이름 51자 / 카테고리 11자 / `latitude=99999` | 전부 **422** + 필드명 | 전부 **500** ❌ |
| 9 | **발급 권한 회귀 방지** | 사장님 B가 A 소유 근무지에 코드 발급 | **403** | ✅ 정상 — 고정할 것 |
| 10 | **fail-closed 회귀 방지** | Redis 끊고 `POST /workplaces/join` | **500** (절대 201 아님) | ✅ 정상 — 우연히 맞은 동작이므로 고정 가치 있음 |

**3번 주의**: 통계 검정(균등 분포, 중복 0건)은 `ThreadLocalRandom`도 통과한다.
**CSPRNG 여부를 증명하지 못한다.** 리플렉션으로 난수원을 확인하거나 `SecureRandom`을
생성자 파라미터로 빼서 목으로 검증할 것.

**2번 주의**: 반드시 실제 DB(Testcontainers 또는 H2)로. Mockito로는 UNIQUE 제약을
검증할 수 없다.

---

## 총평

뼈대가 잘 잡혀 있고 **구멍이 특정한 두 곳에 몰려 있다.**

초대코드의 핵심 설계 결정 셋 — 혼동 문자를 뺀 32자 알파벳, 10분 TTL, `MULTI`로 묶은
양방향 매핑 — 이 전부 옳다. 특히 TTL이 보안 계산의 대부분을 혼자 감당해, 6자리라는
사용자 친화적 길이를 유지하면서도 표적 공격에는 안전하다.

그런데 **읽지 않는 플래그 하나**(`is_accepted`)와 **없는 제약 하나**(`workers` UNIQUE)가
나머지를 무력화한다. C-1은 승인 워크플로 전체를 UI 장식으로 만들고, C-2는 사용자가
버튼을 두 번 누르는 것만으로 그 사람의 근무지를 영구히 500으로 만든다 — 스스로 탈퇴조차
못 한다. C-3의 레이트 리밋 부재가 C-1과 연쇄하면 3시간짜리 무표적 공격이 실제 데이터
접근으로 이어진다.

**세 Critical 모두 수정이 짧다.** C-2는 `ALTER TABLE` 한 줄 + 기존 중복 정리, C-3은
이미 붙어 있는 Redis에 `INCR` + `EXPIRE` 몇 줄, C-1은 모든 경로가 지나가는
`PermissionVerifyUtil` 한 곳에 조건 추가다. 호출부마다 손댈 필요가 없다.
I-4의 `SecureRandom`도 한 줄, I-5의 `setIfAbsent`도 메서드 하나다.
큰 리팩터링이 필요한 상황이 아니다.

Important 중 I-1(PATCH 데이터 소실)이 실사용자 피해가 가장 즉각적이고,
I-8(사장님 탈퇴 CASCADE)이 잠재 피해가 가장 크다. 후자는 코드 수정 전에 Q2 답이 필요하다.
