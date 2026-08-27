# 스코프 6 — 루틴 · 알람 · FCM

- **범위**: `domain/routine/`, `domain/alarm/`, `global/infra/fcm/` (41개 파일)
- **판정**: **수정 후** — 루틴 도메인은 견고하나 **FCM 계층이 푸시 실패를 비즈니스
  트랜잭션에 묶어** 영구 장애를 만든다
- **집계**: Critical 3 / Important 8 / Minor 12 / 미확인 4
- **리뷰 격리**: `docs/review/` 차단. 확정 정책 4건 + 검증 완료 사항 4건만 전제로 제공

> **7개 스코프 중 마지막.** 이로써 전체 코드베이스 리뷰가 완료됐다.

---

## 원장 관리자 검증 — Critical 3건 전부 코드에서 확인

리뷰어 주장을 그대로 싣지 않고 직접 읽었다. **3건 모두 사실이다.**

| 주장 | 검증 결과 |
|---|---|
| `FCMService.sendToSingleUser`에 `@Transactional` + 네트워크 호출 | ✅ `:40` 어노테이션, `:83` `FirebaseMessaging.send()` |
| `acceptWorker`가 FCM을 `updateIsAccepted`보다 **먼저** 하고 실패 시 rethrow | ✅ `WorkerService:292` 전송 → `:294-295` rethrow → `:297` 업데이트. **도달 못 함** |
| `rejectWorker`가 삭제 후 전송 실패로 롤백 | ✅ `WorkerService:306` 삭제 → `:310-311` rethrow |
| `findUserById`가 탈퇴 유저에 예외 | ✅ `UserService:126-128` `AlreadyDeletedException` |
| `users.fcm_token`에 UNIQUE 없음 | ✅ `db/moup.sql:17` `` `fcm_token` TEXT `` — 제약 0건 |
| `FCMTokenService`가 null을 그대로 저장 | ✅ 가드 0줄. `updateFCMTokenByUserId`는 **본문이 비어 있는 흔적**까지 남아 있다 |
| 상한 3개가 `>=` off-by-one | ✅ `:82`, `:333`, `:365`. `:71`만 "생성 전 개수" 비교라 올바르다 |

---

## 잘 된 점

- **루틴 소유권 검증이 쿼리 수준에 박혀 있다.** `RoutineRepository:26,34,42,72,97,104`가
  전부 `WHERE ... AND user_id = #{userId}`. **서비스 계층이 실수해도 남의 루틴을 못 건드린다.**
  이 코드베이스에서 가장 잘 된 권한 설계다 — 다른 도메인은 서비스 계층 검증에만 의존한다.
- **MyBatis 빈 컬렉션 가드가 호출부에 전부 있다.** `IN ()` 문법 에러 경로를 못 찾았고,
  `${}` 문자열 보간은 **전체 0건**이다.
- **관리자 알람 게이트가 이중으로 걸려 있고 실제로 동작한다.** `SecurityConfig:49` URL 레벨 +
  `AdminController:23` 메서드 레벨. `hasRole('ROLE_ADMIN')`의 `ROLE_` 이중 접두사 의혹은
  리뷰어가 `spring-security-core-6.5.1.jar` 바이트코드(`getRoleWithDefaultPrefix`의
  `String.startsWith` 분기)로 확인해 기각했다 — **스코프 7의 동일 판정을 독립 확증.**
- `getAllTodayWorkplaceRoutineCount`(`RoutineService:194-282`)는 쿼리 4번 고정 배치 조회다.
  같은 파일의 N+1 메서드들과 대조적으로 제대로 짰다. **I4의 수정 모델이 이미 파일 안에 있다.**

---

## Critical

### C1 — FCM 전송 실패가 비즈니스 트랜잭션을 롤백시킨다 🔴 **영구 장애**

`FCMService:40`의 `@Transactional` 안에서 `:83`이 네트워크 전송을 한다.
호출부 3곳이 `FirebaseMessagingException`을 잡아 **다시 던져** 같은 트랜잭션을 롤백시킨다.

```java
// WorkerService:291-297 — 순서가 뒤집혀 있다
try {
    fCMService.sendToSingleUser(...);                       // 전송이 먼저
} catch (FirebaseMessagingException e) {
    throw new CustomFirebaseMessagingException(...);        // 롤백
}
workerRepository.updateIsAccepted(workerId, workerUserId, workplaceId, true);  // 도달 못 함
```

| 호출부 | 롤백되는 것 |
|---|---|
| `WorkerService:284-298` `acceptWorker` | 승인 자체가 **실행되지 않는다** (전송이 먼저다) |
| `WorkerService:301-313` `rejectWorker` | 근무자 삭제가 취소된다 |
| `WorkplaceService:346-377` `joinWorkplace` | 근무지 참가 전체(worker + salary 생성)가 무산된다 |

**실패 시나리오 A — 영구 승인 불가.** 알바생이 앱을 삭제·재설치하면 기존 토큰이 FCM에서
`UNREGISTERED`가 된다. **코드 어디에도 `UNREGISTERED`/`INVALID_ARGUMENT`/`SENDER_ID_MISMATCH`를
잡아 죽은 토큰을 지우는 곳이 없다** (`FCMService:58`은 null/blank만 스킵). 사장님이 승인을
누를 때마다 500이 뜨고, 알바생이 재로그인해 토큰을 갱신하기 전까지 **영구히 승인 불가**다.
알바생은 승인이 안 됐으니 근무지가 안 보이고, 사장님은 원인을 알 수 없다.

**실패 시나리오 B — 확정 정책 위반.** `sendToSingleUser`는 `:45`, `:46`에서 발신자·수신자를
모두 `findUserById`로 조회하는데, 이 메서드는 `is_deleted = 1`이면 `AlreadyDeletedException`을
던진다(`UserService:126-128`). 알바생이 탈퇴를 신청하면 **사장님이 그 근무자를 근무지에서
뺄 수 없다.** "탈퇴 유예기간 중 전면 차단"은 **본인의** 기능을 막는 정책인데 코드는
**제3자(사장님)의** 기능까지 막고 있다.

**부수 문제**: 네트워크 호출이 트랜잭션 내부라 FCM 응답 시간 내내 DB 커넥션을 붙잡는다.
FCM 지연 시 HikariCP 고갈로 서비스 전체가 멈춘다.

> **스코프 4 I8의 독립 확증이자 격상.** 스코프 4는 "트랜잭션 내 외부 호출, `acceptWorker`가
> FCM을 DB 업데이트보다 먼저 수행"까지를 Important로 봤다. 스코프 6은 FCM 계층을 통째로 읽고
> **죽은 토큰 정리가 0건**임을 확인해 "일시적 실패"가 아니라 **자가 복구 불가능한 영구 장애**임을
> 밝혀 Critical로 올렸다. 리뷰 도중 고치지 않은 결정이 또 한 번 실증됐다 — 스코프 4에서
> 순서만 바꿨다면 죽은 토큰 문제는 그대로 남았을 것이다.

**수정 방향** (4단계, 순서 의존):
1. `sendToSingleUser`에서 `@Transactional` 제거. DB 저장(`normal_alarms` INSERT)은 호출자
   트랜잭션에, 전송은 `afterCommit` 훅 또는 `@Async`로 커밋 이후에.
2. 전송 실패는 **던지지 말고 로그**. 푸시는 best-effort다. 호출부 3곳의 `catch → throw` 제거.
3. `getMessagingErrorCode()`가 `UNREGISTERED`/`INVALID_ARGUMENT`/`SENDER_ID_MISMATCH`면
   해당 유저의 `fcm_token`을 정리 (`FCMTokenRepository.deleteFCMToken` 재사용).
4. `FCMService:45`의 `sender` 조회는 **반환값이 쓰이지 않는다.** 삭제하면 시나리오 B의
   발신자 측 절반이 함께 사라진다.

### C2 — 하나의 FCM 토큰이 여러 사용자 행에 남아 남의 알림이 배달된다 🔴 **개인정보 노출**

`db/moup.sql:17` `` `fcm_token` TEXT `` — UNIQUE 없음.
`FCMTokenRepository:9`가 다른 행의 동일 토큰을 정리하지 않는다.

```java
@Update("UPDATE users SET fcm_token = #{fcmToken} WHERE id = #{userId}")
```

FCM 등록 토큰은 **앱 설치 단위**로 발급된다. 같은 기기에서 계정을 바꿔 로그인하면
두 사용자 행이 **문자열이 완전히 동일한** 토큰을 갖는다.

**실패 시나리오**: 알바생 A가 친구 폰을 빌려 로그인했다가 **로그아웃하지 않고** 앱만 닫는다
(토큰을 지우는 유일한 경로가 `UserService:211-217`의 `logout`이다). 폰 주인 B가 자기 계정으로
로그인 → `users.B.fcm_token = T`. 그런데 `users.A.fcm_token`도 여전히 `T`다.
이후 A 앞으로 가는 모든 알림이 **B의 잠금화면에 뜬다** — `AlarmContent:10`의
`"%s님이 근무지 참가 요청을 보냈습니다."`는 **요청자 실명을 포함**한다. A의 근무지 이름과
동료 실명이 제3자에게 노출된다.

**수정 방향**: `updateUserFCMToken`을 두 문장으로 만들고 하나의 `@Transactional`에 담는다.
```sql
UPDATE users SET fcm_token = NULL WHERE fcm_token = #{fcmToken} AND id <> #{userId};
UPDATE users SET fcm_token = #{fcmToken} WHERE id = #{userId};
```
근본 대안은 `fcm_tokens(user_id, token, device_id, updated_at)` 별도 테이블(다기기 지원 겸용,
[질문 3](#제품-소유자-결정-필요) 참조)이지만, 지금 필요한 최소 수정은 위 두 줄이다.

### C3 — 로그인에 `fcmToken`이 없으면 기존 토큰을 NULL로 덮어써 푸시가 조용히 끊긴다

`AuthController:135-136`이 `loginRequest.getFcmToken()`을 무조건 저장한다.
`LoginRequest:23-24`에서 `fcmToken`은 `NOT_REQUIRED`이고 `@NotBlank`도 없다.
`AuthController:96`의 `login`에는 **`@Valid`조차 없다.** `FCMTokenRepository:9`는 null을 그대로 쓴다.

**실패 시나리오**: 클라이언트가 재로그인 시점에 FCM SDK로부터 토큰을 아직 못 받았거나
(초기화 지연·네트워크 오류), 신규 가입 전용 필드로 이해해 생략한다 →
`UPDATE users SET fcm_token = null` → 이후 모든 푸시가 `FCMService:58-61`에서
**경고 로그 한 줄 남기고 조용히 스킵**된다. 서버는 200을 반환했고 에러도 없어 **아무도 모른다.**

같은 결함이 `UserService:219-224`에도 있다. **검증 코드가 제거된 흔적이 그대로 남아 있다**:
```java
  public void updateFCMTokenByUserId(Long userId, String fcmToken) {

    

    fcmTokenService.updateUserFCMToken(userId, fcmToken);
  }
```

**수정 방향**: 뿌리에서 한 번 막는다. 호출부 3곳을 각각 고치는 것보다 짧고 앞으로 생길
호출부까지 덮는다.
```java
// FCMTokenService:14
public void updateUserFCMToken(Long userId, String fcmToken) {
    if (fcmToken == null || fcmToken.isBlank()) return;  // 삭제는 deleteUserFCMToken으로만
    fcmTokenRepository.updateUserFCMToken(userId, fcmToken);
}
```

---

## Important

| # | 파일:줄 | 내용 |
|---|---|---|
| **I1** | `RoutineService:412` vs `:436` | **사장님은 알바생 근무의 루틴 목록을 항상 빈 배열로 받는다.** `:412`가 사장님을 권한 통과시켜 놓고 `:436` `findAllByIdListInAndUserId(routineIdList, userId)`는 **요청자(사장님) 소유 루틴만** 조회한다. 루틴은 알바생 개인 소유(`routines.user_id`)라 교집합이 항상 공집합. 200 + `[]`가 나가 "루틴을 안 만들었나 보다"로 오해한다. **정책 결정 필요** |
| **I2** | `FCMService:95-124` | **공지 푸시가 커밋 전에 나가고, 매핑 생성은 미커밋 데이터를 참조하는 별도 스레드에서 돈다.** `:116` `saveAdminAlarm`(미커밋) → `:120` `send()`(푸시 이미 발송) → `:123`이 `@Async @Transactional`(`AlarmService:126-127`)이라 **다른 스레드·다른 트랜잭션**. FK(`db/moup.sql:88`)가 가리키는 부모 행을 못 봐 잠금 대기한다. `:120` 이후 롤백되면 **푸시는 떴는데 앱에서 열면 404**(`AlarmService:152-154`). async 쪽 실패는 `@Async void`라 삼켜진다 |
| **I3** | `AlarmService:35-37`, `:152-154` | **빈 알림함에 404를 던진다.** 신규 가입자가 알림함을 열면 404 → 클라이언트가 "서버 오류"와 "알림 없음"을 구분 못 한다. 목록 조회는 빈 배열 + 200이어야 한다 |
| **I4** | `RoutineService:184`, `:318`, `:444` | **N+1.** `getLinkedWorksFromRoutine`이 루틴당 쿼리 3번. `getAllRoutine`이 루틴마다 호출 → 루틴 20개(상한)면 **61 쿼리**. 코드에 `// N + 1 발생` 주석이 이미 붙어 있다(`:184`). 같은 파일 `getAllTodayWorkplaceRoutineCount`가 올바른 배치 패턴을 보여준다 |
| **I5** | `db/moup.sql:63-72` | **`normal_alarms`에 인덱스·FK가 전혀 없다.** `receiver_id` 인덱스 없어 매 조회가 풀스캔. `ORDER BY`도 페이징도 없어 알림 1만 건이면 1만 행을 통째로 반환. FK가 없어 유저 하드 삭제 시 CASCADE도 안 걸린다 → **탈퇴·삭제된 사용자의 실명이 담긴 알림 본문**(`AlarmContent:10`)이 영구히 남는다 |
| **I6** | `RoutineService:267-271` | **자기모순: 2줄 위에서 null 가드한 값을 그대로 역참조한다.** `:269`는 `getEndTime() != null ? ... : null`, `:271`은 `Duration.between(..., work.getEndTime())`를 무방비 호출. `db/moup.sql:127` `end_time NULL`. 지금은 DTO `@NotNull` 덕에 도달 불가지만 **출퇴근 체크(미퇴근) 기능이 붙는 순간 `GET /routines/today` 전체가 500** |
| **I7** | `AlarmService:137` + `UserRepository:74` | **공지 매핑이 탈퇴자까지 포함하고 전 사용자를 단일 트랜잭션에 담는다.** `findUsersWithPaging`에 `is_deleted` 필터 없음. `:128`의 `@Transactional`이 do-while 전체를 감싸 10만 명이면 10만 INSERT가 한 트랜잭션 |
| **I8** | `RoutineService:82`, `:333`, `:365` | **상한 3개가 off-by-one.** `size() >= 50`이라 "최대 50개까지 생성할 수 있습니다" 메시지를 띄우며 **50개를 거부**한다. 근무당 루틴도 10개가 아니라 9개가 상한. `>`로 바꾸면 된다. (`:71`만 "생성 전 개수" 비교라 올바르다) |

---

## Minor

| # | 파일:줄 | 내용 |
|---|---|---|
| M1 | `RoutineService:157-167` | 방어 코드가 오히려 NPE 유발 — `if (dayName == null)` 블록에 `continue`가 없어 그대로 `dayName.toUpperCase()`로 떨어진다. `work_date NOT NULL`이라 현재 도달 불가 |
| M2 | `RoutineService:164-166` | `catch (IllegalArgumentException e) { // 로그 처리 }` — 주석만 있고 로그가 없다. `@Slf4j`는 이미 붙어 있다(`:52`) |
| M3 | `RoutineService:138-141` | 단일 근무는 `repeat_group_id`가 NULL이라 `IN (NULL)`에 안 걸림 → **반복이 아닌 근무의 `repeatDays`가 항상 빈 배열** |
| M4 | `RoutineService:450-452` + `db/moup.sql:150` | `deleteWorkRoutineMappingByWorkId`는 불필요 — `work_id` FK가 이미 `ON DELETE CASCADE`. `WorkService:927` 호출과 함께 삭제 가능 |
| M5 | `db/moup.sql:44-50` | `routines`에 `UNIQUE (user_id, routine_name)` 없음 → `:77` 확인과 `:90` INSERT 사이 경합으로 동명 루틴 2개. `:71`의 20개 상한도 동일 경합. **스코프 1 I6·스코프 5 C-2와 같은 read-then-write 결함군의 네 번째** |
| M6 | `RoutineService:93-98`, `:343-348` + `db/moup.sql:60` | 요청 내 `orderIndex` 중복 미검증 → `UNIQUE (routine_id, order_index)` 위반 → 422가 아니라 **500** |
| M7 | `RoutineController:104` | `@RequestMapping("/routines")` + `@GetMapping("/works/{workId}/routines")` → 실제 경로가 `/routines/works/{workId}/routines` |
| M8 | `RoutineController:66`, `RoutineSpecification:92-93` | Swagger가 `allowableValues = {"summary"}`(소문자)인데 `ViewType:5`는 `SUMMARY`. Spring 기본 컨버터는 대소문자 구분 → **문서대로 호출하면 400** |
| M9 | `Routine:10`, `RoutineTask:8`, `WorkRoutineMapping:8` | `@NoArgsConstructor` 없어 MyBatis **위치 기반** 생성자 매핑. 컬럼 순서와 필드 순서가 우연히 일치해 동작 중. `SELECT *`(`RoutineRepository:42,64,74`)와 겹쳐 위험 배가. **스코프 4 I6의 독립 확증** |
| M10 | `AlarmRepository:20-22` vs `:42-44` | `saveAdminAlarm`과 `saveAnnouncement`가 완전 동일한 중복. `saveAnnouncement`는 호출부 0건(데드 코드). `AdminAlarmRequest` 전체도 참조 0건 |
| M11 | `AlarmService:129`, `:145`, `FCMService:84`, `:121` | `System.out.println`. 운영 로그로 안 잡힌다. `@Slf4j`는 이미 있다(`FCMService:21`) |
| M12 | `AdminController:63`, `:78` | `@RequestBody`에 `@Valid` 없고 DTO에도 제약 없음 → `title` 생략 시 `NOT NULL` 위반으로 500. `admin_alarm_user_mappings`에 `UNIQUE (alarm_id, user_id)`도 없어 공지 재발송 시 중복 행 |

---

## 미확인 — 확인이 필요한 것

### 1. 🔴 클라이언트가 `ADMIN_ALARM` 토픽을 구독하는가?

**서버 코드 전체에 `subscribeToTopic` 호출이 0건이다.** `FCMService:106`은 발송만 한다.
앱에서 `FirebaseMessaging.subscribeToTopic("ADMIN_ALARM")`을 호출하지 않으면
**전체 공지 푸시가 단 한 명에게도 도달하지 않는다.** 서버는 메시지 ID를 받으므로 실패를
감지할 수 없다. → **클라이언트 코드 확인 필요.**

### 2. 루틴 알람(`routines.alarm_time`)은 누가 발송하는가?

`@Scheduled`/`@EnableScheduling`이 코드베이스에 **0건**이다(`AsyncConfig`에 `@EnableAsync`만).
`alarm_time`을 읽는 서버 코드는 조회 응답 DTO 채우기뿐이다.
클라이언트 로컬 알림 설계면 정상, 서버 발송 의도였다면 **미구현**이다.

### 3. 루틴 완료(체크) 상태는 어디에 저장되는가?

`db/moup.sql` 전체에 `is_done`/`completed`/`checked` 계열 컬럼이 **0건**이다.
"오늘 이 근무의 이 할 일을 완료했다"를 **서버가 보관하지 않는다.**
클라이언트 로컬 저장이면 기기 변경 시 소실된다.

> 부수 소득: 리뷰 축 6번("완료 상태가 근무 재생성 시 어떻게 되는가")의 답이 나왔다 —
> **저장되지 않으므로 잃을 상태가 없다.** `updateMyRecurringWork`의 삭제·재생성은 안전하다.

### 4. `admin_alarm_user_mappings` 배치 INSERT 크기

`AlarmService:28` `BATCH_SIZE = 1000`. MySQL `max_allowed_packet` 기본 64MB에는 여유가
있어 보이나 운영 DB 설정 확인 필요.

---

## 확정 정책 6~9 (Q12~Q16 답변)

### 확정 정책 6 — 루틴 가시성 (Q12 답변)

**루틴은 알바생 개인의 것이다.** 사장님은 알바생 근무에 연결된 루틴의 **제목만** 볼 수 있고
내용(할 일 목록)은 볼 수 없다.

| 주체 | 할 수 있는 것 |
|---|---|
| 알바생 | 자기 루틴 전체 (생성·수정·삭제·근무 연결) |
| 사장님 | 알바생 근무에 연결된 루틴의 **제목만 조회** |

> **"사장님이 자기 루틴을 알바생 근무에 적용"은 하지 않는다** (제품 소유자 결정).
> 검토 중 `WorkService:734`에 `// (사장님은 루틴을 연결하지 않음)` 주석이 있어
> 현재 동작이 의도된 설계임이 확인됐고, 그대로 유지하기로 했다.
> **따라서 I1은 순수 조회 수정이며 쓰기 경로는 건드리지 않는다.**

⚠️ **I1 수정 시 응답에서 빼야 할 것.** `getAllRoutineByWorkRoutineMapping`이 반환하는
`RoutineSummaryResponse`에는 `linkedWorks`(그 루틴이 걸린 **다른 근무들**)가 들어 있다
(`RoutineService:443`). 사장님에게 그대로 주면 **알바생의 다른 근무지 근무까지 노출**된다.
"제목만"은 `routineName`만 남기고 `alarmTime`·`linkedWorks`·할 일 목록을 모두 빼는 것을 뜻한다.

**수정 방향**: `:436`의 `findAllByIdListInAndUserId(routineIdList, userId)`를 요청자가
사장님이면 `findAllByIdListIn(routineIdList)`(소유자 무관)로 분기하고, 사장님 응답은
`routineName`만 담은 축소 DTO로 반환한다. `:412`의 권한 검증은 그대로 둔다.

### 확정 정책 7 — 미승인 근무자의 루틴 연결 금지 (Q13 답변)

`is_accepted = false`인 근무자의 근무에는 루틴을 연결할 수 없다.
`saveWorkRoutineMapping`(`RoutineService:364`)과 `getAllRoutineByWorkRoutineMapping`(`:401`)에
`is_accepted` 검사를 추가한다.

> **`is_accepted` 결함군의 네 번째 지점이다.** 스코프 2 I2 · 스코프 4 C3 · 스코프 5와 함께
> 처리해야 한다 — 코드베이스 전체에 `is_accepted`를 읽는 곳이 0건이었다.

### 확정 정책 8 — 다기기 푸시 지원 (Q14 답변)

`users.fcm_token` 단일 컬럼을 **`fcm_tokens(user_id, token, device_id, updated_at)` 별도
테이블로 분리**하고 `sendEachForMulticast`로 전송한다.

이 전환이 세 문제를 한꺼번에 해결한다:
- **C2** — 토큰 행에 `UNIQUE (token)`을 걸면 기기 재사용 시 이전 소유자 행이 자연히 대체된다
- **로그아웃 부작용** — 지금은 `logout`이 컬럼을 통째로 비워 **폰에서 로그아웃하면 태블릿
  푸시도 죽는다.** 기기별 행이면 해당 기기 것만 지운다
- **C1 3단계** — `UNREGISTERED` 응답 시 그 토큰 행만 삭제하면 된다

### 확정 정책 9 — 푸시는 best-effort, 탈퇴자 관리는 제3자에게 열어둔다 (Q15·Q16 답변)

- **푸시 전송 실패를 사용자 에러로 보이지 않는다.** "승인은 되었으나 알림 발송에 실패"가
  올바른 동작이다. `normal_alarms` 히스토리가 남아 앱 내 알림함에서 확인할 수 있다.
- **탈퇴 신청 중인 알바생도 사장님이 근무지에서 뺄 수 있어야 한다.** "탈퇴 유예 중 전면
  차단"은 **본인의** 기능을 막는 정책이며, **제3자(사장님)의** 기능까지 막는 것은 아니다.

→ C1의 수정 방향 4단계가 그대로 확정됐다.

---

## 제품 소유자 결정 필요 — ✅ 답변 완료

| # | 질문 | 관련 |
|---|---|---|
| **Q12** ✅ [정책 6](#확정-정책-6--루틴-가시성적용-범위-q12-답변) | **사장님이 알바생의 루틴을 볼 수 있어야 하는가?** (a) 열람 가능 → `findAllByIdListIn`(소유자 무관)으로 바꾼다, (b) 알바생만 본다 → `:412`에서 사장님을 막는다. 지금은 **권한은 통과시키고 데이터는 안 주는** 어중간한 상태다 | I1 |
| **Q13** ✅ [정책 7](#확정-정책-7--미승인-근무자의-루틴-연결-금지-q13-답변) | **미승인 근무자(`is_accepted = false`)의 루틴 연결을 허용하는가?** `saveWorkRoutineMapping`(`:364`), `getAllRoutineByWorkRoutineMapping`(`:401`) 어디에도 `is_accepted` 확인이 없다 | 스코프 2 I2·스코프 5의 `is_accepted` 결함군 |
| **Q14** ✅ [정책 8](#확정-정책-8--다기기-푸시-지원-q14-답변) | **다기기 푸시를 지원해야 하는가?** 현재 `users.fcm_token` 단일 컬럼이라 마지막 로그인 기기만 받는다. 더 나쁜 것은 `logout`이 컬럼을 통째로 비워 **폰에서 로그아웃하면 태블릿 푸시도 죽는다** | C2 |
| **Q15** ✅ [정책 9](#확정-정책-9--푸시는-best-effort-탈퇴자-관리는-제3자에게-열어둔다-q15q16-답변) | **푸시 전송 실패를 사용자 에러로 보여야 하는가?** 원장 관리자 의견은 **아니오** — 푸시는 best-effort이고 `normal_alarms` 히스토리가 남아 앱 내 알림함에서 확인 가능하다. "승인은 되었으나 알림 발송 실패"가 맞다 | C1 |
| **Q16** ✅ [정책 9](#확정-정책-9--푸시는-best-effort-탈퇴자-관리는-제3자에게-열어둔다-q15q16-답변) | **탈퇴 신청 중인 알바생을 사장님이 근무지에서 뺄 수 있어야 하는가?** 확정 정책("탈퇴 유예 중 전면 차단")은 **본인** 기능을 막는 취지였는데, 코드가 **제3자(사장님)** 기능까지 막고 있다 | C1 시나리오 B |
