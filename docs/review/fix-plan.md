# 수정 계획 (C 단계)

> 7개 스코프 리뷰가 모두 끝났다. [README](README.md)의 "수정 단계 진입 조건"에 따라
> 전체 findings를 **파일·주제 단위로 재그룹핑**하고 의존 순서를 확정한다.
> 이 문서는 **무엇을 어떤 순서로 고칠지**만 정한다. 각 finding의 근거는 스코프 문서에 있다.

## 규모

| | Critical | Important | Minor |
|---|---|---|---|
| 원 집계 (7개 스코프 합) | 31 | 71 | 82 |
| 이미 수정 완료 | −7 | −5 | −1 |
| 스코프 간 중복 (아래 참조) | −9 | −11 | −4 |
| **남은 고유 항목** | **15** | **55** | **77** |

### 중복 제거 — 같은 결함을 여러 스코프가 짚은 것

| 결함 | 짚은 스코프 | 대표로 삼을 것 |
|---|---|---|
| `is_accepted` 미검사 | 2 I2 · 4 C3 · 5 C-1 · 6 정책 7 | **5 C-1** (전수 확인 + 스키마까지) |
| 음수 `restTimeMinutes` | 2 C1 · 3 C-5 · 4 C5 | **2 C1** |
| `workplaces.owner_id` CASCADE | 4 C4 · 5 I-8 · 7 C3 | ✅ 수정 완료 |
| `PermissionVerifyUtil` NPE | 2 I3 · 4 #12 · 7 C2 | ✅ 수정 완료 |
| FCM이 트랜잭션 안에서 네트워크 호출 | 2 C5 · 4 I8 · 5 I-6 · 6 C1 · 7 I3 | **6 C1** (영구 장애까지 규명) |
| 죽은 FCM 토큰 미정리 · 다기기 불가 | 6 C2 · 7 I4 | **6 C2** + 정책 8 |
| `ADMIN_ALARM` 구독자 0명 | 6 미확인1 · 7 I5 | **7 I5** |
| `@NoArgsConstructor` 부재 | 4 I6 · 5 I-9 · 6 M9 | **4 I6** |
| 동료 급여 노출 | 2 I1 · 4 I5 | **2 I1** |
| `(worker_id, work_date)` 인덱스 | 2 M7 · 7 I10 | **7 I10** |
| `workers` UNIQUE 부재 | 5 C-2 · 7 I11 | **5 C-2** |
| 레이트 리밋 부재 | 5 C-3 · 7 I17 | **5 C-3** |
| 근무 시간 상한 없음 (DoS) | 2 I7 · 3 I-10 | **2 I7** |
| `normal_alarms` 인덱스·FK 부재 | 6 I5 · 7 I9 | **6 I5** |
| `hourly_rate` NULL 언박싱 | 2 M11 · 3 I-2 | **3 I-2** |

---

## ✅ Phase 0 — 완료 (`980fcd2` · `135b966` · `572327c`)

### 0-1. `@NoArgsConstructor` 부재 (4 I6 · 5 I-9 · 6 M9) ✅ `980fcd2`

**확인 완료**: 엔티티 10개 중 **9개**에 `@NoArgsConstructor`가 없다.
`User`만 있다. `mybatis.configuration.arg-name-based-constructor-auto-mapping`도
설정돼 있지 않다(`application.properties:16-17`에 `map-underscore-to-camel-case`만).

```
Worker 0 · Workplace 0 · Salary 0 · Work 0 · Routine 0
RoutineTask 0 · WorkRoutineMapping 0 · UserToken 0 · SocialToken 0 · User 1
```

MyBatis는 무인자 생성자가 없으면 **위치 기반** 생성자 매핑으로 떨어진다.
지금은 컬럼 순서와 필드 순서가 우연히 맞아 동작한다.

**왜 Phase 0인가**: 이 계획에는 **컬럼을 추가하는 작업이 3건**(`is_accepted` 제약,
`withdrawn_at`, `fcm_tokens`) 있다. 열 순서와 필드 순서가 어긋나는 순간
**예외가 아니라 조용히 잘못된 값**이 들어간다 — 급여 필드에 근무지 ID가 들어가는 식이다.
테스트가 없으면 못 잡는다.

**적용**: 엔티티 9개에 `@NoArgsConstructor` + `@AllArgsConstructor`.
전역 설정(`arg-name-based-constructor-auto-mapping=true`) 한 줄이 더 짧지만
**`User`가 이미 쓰는 패턴을 따랐다** — 코드베이스 일관성이 있고,
`-parameters` 컴파일 옵션 유지에 의존하지 않는다.

> `@Builder`는 무인자 생성자가 생기면 전체 인자 생성자를 자동 생성하지 않으므로
> `@AllArgsConstructor`를 함께 붙여야 한다.

**검증**: `EntityConstructorMappingTest` 10건. **변이 테스트로 실효성 확인** —
`Worker`에서 애노테이션을 빼면 해당 케이스가 실패한다.

### 0-2. 음수 `restTimeMinutes` 하한 (2 C1 · 3 C-5 · 4 C5) ✅ `135b966`

요청 DTO **4곳**에 `@PositiveOrZero` (나머지 4곳은 응답 DTO·엔티티라 대상 아님).
컨트롤러 6개 엔드포인트에 `@Valid`가 이미 걸려 있어 즉시 동작한다. **급여 계산을 손대기 전에** 해야 한다 — 안 그러면
Phase 3의 회귀 테스트가 음수 입력을 정상으로 가정한 기대값에 고정된다.

### 0-3. 상한 off-by-one 3건 (6 I8) ✅ `572327c`

`>=` → `>`. `RoutineService:82`, `:333`, `:365`. 한 글자씩 3곳.
Phase 0에 넣는 이유는 **다른 것과 충돌하지 않고 지금 안 하면 잊혀서**다.

---

## ✅ Phase 1 — 완료 (`f5f8cba`와 함께 `db/moup.sql`·`db/init/moup.sql`에 반영)

Phase 0-1 완료가 **전제**다. 흩어서 하면 운영 DB에 `ALTER`를 여러 번 치게 되니 묶는다.

| 대상 | 근거 | 내용 |
|---|---|---|
| `workers` | 5 C-2 · 7 I11 | `UNIQUE (workplace_id, user_id)` — 중복 참여 경합 차단 |
| `workers.is_accepted` | 5 C-1 | `NOT NULL DEFAULT 0` + **백필**([정책 12](#확정-정책-12--is_accepted-백필-d1-답변)) — NULL보다 기존 `false` 행이 더 위험하다 |
| `works` | 7 I10 · 2 M7 | `INDEX (worker_id, work_date)` — 13개월 캘린더 filesort 제거 |
| `normal_alarms` | 6 I5 · 7 I9 | `INDEX (receiver_id, sent_at DESC)` + `receiver_id`/`sender_id` FK CASCADE |
| `routines` | 6 M5 | `UNIQUE (user_id, routine_name)` |
| `workplaces` | 5 I-7 | `UNIQUE (owner_id, workplace_name)` |
| `admin_alarm_user_mappings` | 6 M12 | `UNIQUE (alarm_id, user_id)` |
| `salaries` | 7 I14 | `CHECK` — `HOURLY`인데 `hourly_rate IS NULL` 방지 |

`is_accepted` 백필은 [확정 정책 12](#확정-정책-12--is_accepted-백필-d1-답변)로 확정됐다.
⚠️ **Phase 2의 최대 배포 위험** — 게이트를 켜면 승인받지 않은 채 일해 온 사람이 차단된다.

---

## ✅ Phase 2 — 완료 (`f5f8cba`)

**코드베이스 전체에서 `is_accepted`를 읽는 곳이 0건**이다. 승인 절차가 장식이다.

Phase 1(스키마) 완료가 전제. 영향 파일이 가장 넓다:
`WorkService` · `WorkerService` · `WorkplaceService` · `RoutineService` · `SalaryService`

**[확정 정책 6](scope-5-workplace.md#확정-정책-6--승인-대기-가시-범위-q1-답변)** — 승인 대기 중 가시 범위:
(a) 근무지 이름·주소 **허용** / (b) 자기 급여 설정 **허용** /
(c) 근무 등록·출퇴근 **차단** / (d) 목록 표시 **허용하되 `PENDING_APPROVAL`로 구분**

**[확정 정책 15](scope-6-routine-alarm-fcm.md#확정-정책-15--미승인-근무자의-루틴-연결-금지-q13-답변)** —
미승인자의 근무에는 루틴을 연결할 수 없다.

이걸 고치면 **4 I5 / 2 I1(동료 급여 노출)의 절반이 함께 닫힌다** — 미승인자의 접근이 먼저 막힌다.

### 적용 결과

`PermissionVerifyUtil.verifyWorkerPermission`이 `Worker` 객체를 받아 승인 여부까지 확인한다.
**이름을 기본형으로 둔 것이 설계 결정이다** — 새 호출부가 실수로 골라도 안전한 쪽이 된다.
승인 대기 중에도 허용해야 하는 것만 `verifyWorkerIdentityAllowingPending`을 쓴다.

| 경로 | 정책 6 항목 | 판정 |
|---|---|---|
| `WorkService` 5곳 (근무 등록·출퇴근·수정·삭제·조회) | (c) | **차단** |
| `RoutineService:416` (루틴 연결·조회) | 정책 15 | **차단** |
| `WorkerService.updateMyWorker` (자기 급여 설정) | (b) | 허용 |
| `WorkerService.deleteMyWorker` (참여 취소) | — | 허용. **막으면 대기자가 갇힌다** |
| `WorkplaceService` 목록·단건 | (a)(d) | 허용하되 `status`로 구분 |

**`WorkplaceStatus` 열거형 하나로 정책 6과 정책 5(사장님 탈퇴)를 함께 만족시킨다.**
`owner_id IS NULL`이 곧 사장님 탈퇴 신호이므로 **`withdrawn_at` 컬럼이 필요 없다**
— Phase 6의 DDL이 사라졌다.

`WorkerSummaryResponse.isAccepted`도 추가했다. 이 필드가 없어 **사장님이 누가 대기자인지
알 수 없고 승인 버튼을 어디에 띄울지 판단할 수 없었다.**

회귀 테스트 7건, 변이 테스트로 실효성 확인. **전체 58건 통과.**

---

## 🔶 Phase 3 — 3-1~3-7 완료, **3-8(세금)만 남음**

한 파일(`SalaryCalculationService`, 705줄)에 집중된다. **내부 순서 의존이 강하다.**

```
3-1 ✅ C-5  야간 분에 휴게시간 비례 배분 → 주간 근무시간 음수 해소
    ✅ M-8  netMinutesOf 헬퍼로 주간·월간 합계의 음수 클램프 통일
3-2 ✅ C-4  야간 분을 수당 설정과 분리 (사실 기록)
3-3 ✅ M-1  정수 연산 전환 (부동소수점 절삭 90,310건)
    ✅ M-2  배분 나머지를 마지막 근무일에 몰아준다
3-4 ✅ C-2  분자·분모를 payableWorks 한 집합에서 뽑는다
    ✅ I-2  그 주 마지막 non-null 시급 (확정 정책 4)
3-5 ✅ C-1  법정 산식 min(주 소정근로시간/40, 1.0) × 8 × 시급
3-6 ✅ C-6  재계산 범위 (+ 2 C4) — Phase 5와 함께
3-7 ✅ C-7  수당 플래그가 스냅샷 아닌 현재값을 읽음 (확정 정책 3 위반)
       C-3  이미 호출 제거로 확정
3-8 ⬜ I-6  근로소득 과세 전환 — 간이세액표 파일 + D6 대기
```

### 실측 — 예전 산식이 얼마나 틀렸나

| 주 근무 | 예전 | 법정 | 배율 |
|---|---|---|---|
| 40시간 / 5일 | 80,000 | 80,000 | 1.0 |
| 24시간 / 3일 | 80,000 | 48,000 | 1.67배 |
| 20시간 / 2일 | 100,000 | 40,000 | **2.5배** |
| 48시간 / 4일 | 120,000 | 80,000 | 1.5배 (8시간 상한) |

**주 5일 근무일 때만 우연히 맞았다.**

회귀 테스트 16건 추가. 변이 테스트로 실효성 확인 — 8시간 상한을 빼면 1건,
나머지 배분을 없애면 1건이 실패한다. **전체 74건 통과.**

```
3-8  I-6  근로소득 과세로 전환 (현재 사업소득 3.3% + 지방세 10% = 3.63%)
          정책 13(간이세액표 조회)으로 방식 확정. 표 파일 + D6 결정이 남았다.
```

---

## ✅ Phase 4 — 완료 (`899855c` · `2695f69`)

```
4-1 ✅ fcm_tokens(user_id, token, updated_at) 분리 · users.fcm_token 제거
       UNIQUE (token) + upsert → 기기 재사용 시 토큰 소유자가 새 유저로 넘어간다
       로그아웃에 선택 fcmToken → 해당 기기만 끊는다
4-2 ✅ FCMTokenService 빈 값 가드
4-3 ✅ @Transactional 제거 · afterCommit 전송 · 호출부 3곳 catch→throw 제거
       미사용 sender 조회 제거 (탈퇴자 관리 차단 해소)
4-4 ✅ UNREGISTERED/INVALID_ARGUMENT/SENDER_ID_MISMATCH → 토큰 행 삭제
4-5 ⚠️ 멀티캐스트 대신 기기별 send() 루프 — 아래 참조
4-6 ✅ 공지 매핑을 푸시보다 먼저, 같은 트랜잭션에서 (@Async 제거)
4-7 ✅ 토큰 등록 시 서버가 ADMIN_ALARM 토픽 구독
```

### 4-5 정정 — `sendEachForMulticast`를 쓸 수 없다

`build.gradle:44`가 **firebase-admin 8.1.0**이다. 이 버전에는 `sendEachForMulticast`가 없고,
있는 `sendMulticast`/`sendAll`은 **2024년 6월에 종료된 레거시 batch 엔드포인트**를 쓴다.

→ 기기별 `send()` 루프로 구현했다. 사용자당 기기가 1~3대라 실무상 문제가 없고,
개별 오류 코드를 그대로 받아 죽은 토큰 정리가 오히려 단순해진다.
코드에 `ponytail:` 주석으로 상한과 업그레이드 경로를 남겼다.

> **신규 인프라 항목**: firebase-admin 8.1.0은 2021년 버전이다. 업그레이드 검토가 필요하다.
> Phase 9에 추가.

### 부수 발견 — 승인·거부 알림 제목이 enum 이름 그대로 나가고 있었다

`WorkerService`가 `AlarmTitle.X.toString()`을 써서 푸시 제목과 앱 내 알림 목록에
`ALARM_TITLE_WORKPLACE_JOIN_ACCEPTED`가 그대로 노출됐다. 같은 enum을
`WorkplaceService:356`은 `.getTitle()`로 쓴다. `getTitle()`로 통일했다.
**리뷰 7개 스코프 어디에도 없던 건이다.**

### 테스트 — 세션 시작 이래 처음으로 전체 초록

기존 실패 6건은 전부 **매처와 생 `null`을 섞어** `InvalidUseOfMatchersException`이 난 것이었고,
미완성 스텁이 다음 테스트로 새어 관계없는 케이스까지 무너뜨리고 있었다.
FCM 실패 시 롤백을 검증하던 2건은 계약이 바뀌었으므로
**"승인·참가가 푸시보다 먼저 확정된다"** 는 순서 검증(`InOrder`)으로 다시 썼다.
**51건 전부 통과.**

**4-1이 4-4보다 먼저**여야 한다 — 단일 컬럼 구조에서는 "죽은 토큰만 삭제"가 불가능하다.

---

## ✅ Phase 5 — 완료 (`061ac81` · `6d37c15`)

- **C3** ✅ 반복 시작일이 기준 근무일보다 앞서면 거부. 삭제 기준일을 앞당기는 대안은
  **사용자가 요청하지 않은 과거 근무를 조용히 지우므로** 택하지 않았다
- **C4 / 3-6** ✅ `recalculateWeeksInRange` 헬퍼로 세 지점을 고쳤다.
  `updateSingleWorkInternal`은 수정 **전** 근무일을 몰라 파라미터 추가가 필요했다
- **C5** ✅ `WorkService.clockIn` 단일 트랜잭션으로 통합
- **M1** ✅ 죽은 `stopRecurrenceAndUpdateSingle` 제거 — 같은 재계산 결함을 갖고 있어
  남겨두면 되살릴 때 함정이 된다

---

## Phase 6 — 사장님 탈퇴 시 가명처리 (4 C4 · 확정 정책 5)

**DDL이 필요 없다.** `owner_id IS NULL`이 곧 "사장님이 하드 삭제됐다"는 신호이고
(FK가 `ON DELETE SET NULL`), Phase 2에서 `WorkplaceStatus.OWNER_WITHDRAWN`으로
이미 표시하고 있다.

남은 것은 **접근 차단 범위**다 — 정책 5의 "접근만 차단"을 어디까지 적용할지.
`PermissionVerifyUtil`에 `OWNER_WITHDRAWN`일 때의 처리를 넣을지가 핵심이고,
[스코프 5의 남은 결정 2건](scope-5-workplace.md)이 여기에 걸려 있다:
알바생이 자기 과거 근무·급여를 계속 볼 수 있어야 하는가, 유예 3일 중 동작은 어떠한가.

---

## ✅ Phase 7 — 완료 (`94b7509`)

Redis가 이미 있어 새 의존성 없이 구현했다. **실패한 조회만 센다** — 정상 사용자는
코드를 한두 번 잘못 입력할 뿐이고 무차별 대입은 정의상 실패가 압도적이다.
성공하면 카운터를 지우고, 한도 도달 시에는 **Redis 조회조차 하지 않는다**
(404 오라클 자체가 정보다). 10분 20회로 무표적 공격 기대 소요가 100배가 된다.

`/auth/**`의 IP 단위 제한(7 I17의 나머지 절반)은 **nginx/ALB에 두는 것이 맞다** →
Phase 9로 이관.

---

## ✅ Phase 8 — 완료 (`9de4ea6` · `545eb30` · `400cc16` · `ec114cc`)

| 항목 | 출처 | 커밋 |
|---|---|---|
| checked 예외가 핸들러를 통과해 Boot 기본 `/error`로 나감 | 7 I6 | `9de4ea6` |
| `ErrorCode.INVALID_TOKEN`이 401이 아니라 400 | 7 I16 | `9de4ea6` |
| CORS 두 곳에 다르게 정의 · Security 쪽에 PATCH 누락 | 7 I12 | `9de4ea6` |
| DTO 검증이 스키마 제약 미반영 (422여야 할 것이 500) | 5 I-3 · 3 I-3 · I-4 · 4 I9 | `9de4ea6` |
| 빈 알림함에 404 | 6 I3 | `9de4ea6` |
| 인증 실패가 401이 아니라 500 | 1 I3(b) | `9de4ea6` |
| 파일 업로드가 클라이언트 `Content-Type`만 신뢰 | 7 I7 | `545eb30` |
| 프로필 이미지 교체 시 삭제가 업로드보다 먼저 | 7 I8 · 4 I8 | `545eb30` |
| refresh token 평문 저장 | 1 I7 | `545eb30` |
| 알바생 경로가 0행 갱신 후 204 반환 | 5 I-2 | `400cc16` |
| `PATCH`가 전체 치환으로 동작해 주소·좌표를 지움 | 5 I-1 | `ec114cc` |

### I-1 — 부분 갱신으로 갈지 PUT으로 이름만 바꿀지

클라이언트를 확인한 결과 5개 필드를 **항상 전송**하고 있었다(타입이 non-optional).
누락 경로가 없으니 NULL 파괴는 일어나지 않았지만, 대신 `"기본 주소"`/`0.0`이라는
더미 값이 나가고 있었다 — NULL보다 나쁘다. nullable 체크로 안 걸러지고 유효해 보인다.

**PUT으로 이름만 바꾸는 안을 버렸다.** `address`/`latitude`/`longitude`는 스키마상
NULL 허용이고 생성 시에도 선택이라 "값 없는 근무지"가 정당한 상태다. 필수로 만들 수
없으니 PUT으로 바꿔도 누락 → NULL 경로가 그대로 남는다. 이름만 정직해지고 안전해지지
않는다.

부분 갱신의 실제 값어치는 미래 방어가 아니라 **지금 클라이언트의 타입 문제 해결**이다.
클라이언트가 `address: String`(non-optional)이라 서버의 nil을 표현할 수 없어 뭔가
지어내야 했고, 그게 `"기본 주소"`가 태어난 이유다. 부분 갱신이면 `String?` + 키 생략으로
"건드리지 않음"을 표현할 수 있다. 어느 쪽이든 클라이언트를 고쳐야 하는데, 부분 갱신에서만
그 자연스러운 수정이 안전하다.

### 부수 발견 — `@JsonTypeInfo(DEDUCTION)`은 판별 키 충돌 시 키 순서로 결정한다

`ownerBasedLabelColor`와 `workerBasedLabelColor`가 **둘 다 존재하면 예외가 아니라
JSON에서 먼저 나온 키**로 서브타입이 정해진다. 클라이언트가 `null`을 명시 출력하는
인코더로 바뀌면 요청이 엉뚱한 타입으로 역직렬화된다.

`updateWorkplace`가 `user.getRole()`과 판별된 타입을 교차 검증하므로 오판별은 403으로
드러나고 실害는 없다. `WorkplaceUpdateDeductionTest`로 이 동작을 고정했다.

---

## 🔶 Phase 9 — 코드 작업 완료, Firebase 키만 남음

인프라·배포를 건드리므로 `develop` 직접 커밋이 아니라 이 브랜치에 쌓았다
(라이브러리 버전 업그레이드만 서버 코드로 보아 `develop`에 넣었다).
**`deploy.yml`이 develop push에 트리거되므로, 배포 파이프라인 자체를 바꾸는
변경을 자동 배포시키지 않기 위해서다.**

| 항목 | 출처 | 커밋 |
|---|---|---|
| 🔴 **Firebase 키 폐기·재발급** | 7 C7 | ⬜ **미결 — 코드 아님. 사용자 작업** |
| `manual-db-init.yml`이 확인 절차 없이 프로덕션 DB를 날림 | 7 C4 | ✅ `1c09428` |
| 프로덕션이 develop 이미지를 가져감 | 7 C6 | ✅ `575b780` |
| 프로파일 분리 부재 · Docker 로그 로테이션 없음 | 7 I2 | ✅ `575b780` |
| `/health`가 DB·Redis 미확인 | 7 I13 | ✅ `3c08fcd` |
| 배치 실행 경로가 버전 관리 밖 | 7 C5 · INF-1·2 | ✅ `3827543` |
| `/auth/**` IP 단위 제한 (Phase 7에서 이관) | 7 I17 | ✅ `61e273b` |
| firebase-admin 8.1.0 업그레이드 | Phase 4에서 발견 | ✅ `b68fe44` (develop) |

### C4 — 문서보다 심각했다

`db/moup.sql`의 첫 줄이 `DROP SCHEMA IF EXISTS moup;`다. 이 워크플로는 DB를 통째로
날리는데 확인 절차가 하나도 없었고 더미 데이터 삽입이 기본값 `true`였다. 저장소 쓰기
권한만 있으면 두 번의 클릭으로 프로덕션 DB가 사라졌다.

확인 문구 입력(러너에서 먼저 검사 — 틀리면 서버 접속조차 안 한다)과 DROP 직전
mysqldump 백업(실패하거나 결과가 비면 중단)으로 막았다. 실수뿐 아니라 고의로 눌러도
복구된다.

### C5 — 배치가 한 번도 돈 적이 없었다

`delete_old_users.log`에 남은 건 이것뿐이다:

```
/bin/bash: /home/neoskyclad/MOUP-Server/src/main/resources/delete_old_users.sh:
No such file or directory
```

cron이 가리키는 경로에 스크립트가 없고(경로도 틀렸다 — `server/`가 빠져 있다), 그
crontab을 등록하는 `deploy.yml` 블록마저 통째로 주석 처리돼 있었다. **탈퇴한 사용자의
데이터가 유예기간이 지나도 계속 남아 있었다.**

계획은 스크립트를 버전 관리에 넣는 것이었으나 **`@Scheduled`로 앱 안에 옮겼다.**
기존 구조는 스크립트 위치 · crontab 등록 · `ADMIN_AUTH_TOKEN` 유효성 · 서버 URL
네 가지가 모두 일치해야 동작하고 하나만 어긋나도 조용히 멈춘다. 실제로 셋이 어긋나
있었다. 앱 안에서는 넷 다 필요 없고, 배포 전제조건이던 "`ADMIN_AUTH_TOKEN` 재발급
안 하면 cron이 401"도 사라진다.

### C6 — `export TAG`가 죽은 코드였다

Jenkinsfile은 develop → `develop-N`+`latest`, main → `main-N`+`stable`로 태그를
나눠 붙이고 배포 직전에 `export TAG=...`까지 한다. 그런데 두 compose 파일 모두
`image: neoskycladdocker/moup`(태그 없음)이라 항상 `:latest`를 당겼다.
**main 배포가 develop 빌드를 가져갔다.**

### I17 — real_ip 없이 레이트 리밋을 걸면 자폭이다

prod nginx는 NPM 뒤에 있어 `$remote_addr`이 항상 NPM의 컨테이너 IP다. real_ip 설정
없이 IP 단위 제한만 걸면 전체 사용자가 버킷 하나를 공유해 서비스가 통째로 막힌다.
사설 대역만 신뢰하면 되는 이유는 이 서버가 호스트에 포트를 열지 않아 `proxy-net`
밖에서는 접속 자체가 불가능하기 때문이다.

함께 수정 — prod의 `client_max_body_size`가 `0`(무제한)이었다. 스프링이 10MB에서
거부해도 nginx가 본문을 전부 받아 버퍼링한 뒤라, 거부되기 전에 SD 카드가 먼저 찬다.

### 배포 시 눈으로 확인할 것

1. `/health` 응답 본문이 `"OK"` → `{"status":"UP"}`으로 바뀐다. 본문 문자열을 검사하는
   모니터가 있다면 상태 코드 검사로 바꿔야 한다.
2. compose가 `${TAG}`를 읽게 됐다. 의도한 태그의 이미지가 실제로 당겨지는지.
3. 기동 로그에 Firebase 초기화 성공 — 메이저 버전 점프라 컴파일 통과가 런타임 호환을
   보장하지 않는다. `FCMConfig`는 모든 테스트가 Mockito라 한 번도 실행되지 않는다.
4. `nginx -t` — 로컬에 docker·nginx가 없어 crossplane(nginx 공식 파서)으로 문법과
   지시자 컨텍스트만 확인했다.
5. `@Scheduled` 배치 등록 (매일 04:00 KST).

---

## Phase 10 — 신규 개발 (리뷰 findings 아님)

리뷰 과정에서 **미구현임이 드러난 것**들. 결함 수정이 아니라 개발이므로 Phase 0~9와 분리한다.

### 10-1. 루틴 완료(체크) 상태 서버 보관 ✅ 진행 결정 (D4 답변)

`db/moup.sql` 전체에 `is_done`/`completed`/`checked` 계열 컬럼이 **0건**이다.
"오늘 이 근무의 이 할 일을 완료했다"를 서버가 보관하지 않아 **기기를 바꾸면 사라진다.**

```sql
CREATE TABLE `routine_task_completions` (
    `id`              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `work_id`         BIGINT   NOT NULL,
    `routine_task_id` BIGINT   NOT NULL,
    `completed_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `uk_completion` (`work_id`, `routine_task_id`),
    FOREIGN KEY (`work_id`)         REFERENCES works (`id`)         ON DELETE CASCADE,
    FOREIGN KEY (`routine_task_id`) REFERENCES routine_tasks (`id`) ON DELETE CASCADE
);
```

**규모는 작다.** 테이블 1개 + 토글 API 1개 + 조회 응답에 필드 추가.

⚠️ **Phase 0-1 필수 전제** — 신규 테이블이고 기존 엔티티에 필드가 추가되므로,
위치 기반 매핑 상태에서 하면 조용히 어긋난다.

⚠️ **`updateMyRecurringWork`가 근무를 삭제·재생성한다.** 지금은 잃을 상태가 없어서
무해했지만, 완료 상태가 생기면 `work_id` CASCADE로 **근무 수정 시 체크가 전부 날아간다.**
Phase 5(반복 근무 C3)와 함께 설계해야 한다.

### 10-2. 루틴 알람 서버 발송 ❓ D3 확인 대기

`alarm_time` 참조를 전수 확인했다 — **INSERT · ORDER BY · UPDATE · 조회 응답 채우기가 전부이고
발송 경로가 없다.** `@Scheduled`/`@EnableScheduling`도 코드베이스에 0건이다.

> **개발 중 받아본 FCM 알림과는 다른 경로다.** 서버의 푸시 발송은 5곳뿐이며
> 그중 4곳이 토큰 기반 개별 발송(참가 요청·승인·거부·관리자 개별)이고 정상 동작한다.
> 루틴 알람은 그 어느 것도 아니다.

서버 발송이 요구사항이면 필요한 것:
- 스케줄러(`@Scheduled` 또는 Pi cron) — 분 단위로 `alarm_time` 스캔
- **중복 발송 방지** — 서버가 2대 이상이면 같은 알람을 여러 번 쏜다
- 사용자 타임존은 `Asia/Seoul` 고정이므로 이 부분은 단순하다

---

## 확정 정책 10~12 (Q5·Q9·D1 답변)

### 확정 정책 10 — 알바생 소득은 **근로소득** (Q5 답변)

**현재 코드는 사업소득 방식이며, 그 위에 지방소득세를 한 번 더 더한다.**

```java
// SalaryCalculationService:687-690
incomeTax      = (int) (grossIncome * incomeTaxRate);  // salary.rates.simple-income-tax=0.033
localIncomeTax = (int) (incomeTax * 0.1);
```

`3.3%`는 **사업소득 원천징수율**이고 그 자체가 이미
`소득세 3% + 지방소득세 0.3%`를 합친 값이다. 여기에 10%를 또 더해 실효 **3.63%** 를 뗀다.

**근로소득은 요율이 아니라 `근로소득 간이세액표`(소득세법 시행령 별표2) 조회다.**
월급여액과 공제대상 가족 수로 결정되며, **월급여 106만원 미만 구간은 소득세 0원**이다.
지방소득세는 그 소득세의 10%다(소득세가 0이면 0).

→ 대부분의 알바생에게 **현재 코드는 0원이어야 할 세금을 3.63% 떼고 있다.**
I-6은 "이중 공제"가 아니라 **과세 모델 자체가 틀린 것**이다. 심각도 Important → **Critical**.

**구현 선택지** (제품 소유자 결정 필요 → [D5](#미결---남은-결정)):

| 안 | 내용 | 정확도 | 비용 |
|---|---|---|---|
| a | 간이세액표를 테이블/리소스로 적재해 조회 | 정확 | 표 데이터 관리 + 연 1회 갱신 |
| b | 106만원 미만 0원 + 그 이상만 근사 요율 | 대부분 정확 | 작음 |
| c | 소득세 공제를 표시하지 않고 세전만 보여줌 | 오해 없음 | 가장 작음 |

`hasIncomeTax` 플래그가 이미 있으므로 어느 안이든 껐을 때 0원은 유지된다.

### 확정 정책 11 — 주휴수당 법정 산식 (Q9 답변)

**법정 산식** (근로기준법 시행령 제9조 제1항 별표2):

```
주휴수당 = min(주 소정근로시간 ÷ 40, 1.0) × 8 × 시급      (주 15시간 이상일 때만)
```

**현재 코드** (`SalaryCalculationService:103-104`):
```java
double avgDailyWorkHours = (weeklyWorkMinutes / 60.0) / weekWorks.size();
weeklyHolidayAllowance = (int) (avgDailyWorkHours * weekWorks.get(0).getHourlyRate());
```
= `(주 총 근로시간 ÷ 근무일수) × 시급` — **1일 평균 근무시간에 시급을 곱한다. 8시간 상한도 없다.**

**주 5일 근무일 때만 우연히 일치한다.**

| 주 근무 형태 | 현재 코드 | 법정 | 배율 |
|---|---|---|---|
| 40시간 / 5일 (8h×5) | 80,000 | 80,000 | 1.0 ✅ |
| 20시간 / 2일 (10h×2) | **100,000** | 40,000 | **2.5배** |
| 24시간 / 3일 (8h×3) | 80,000 | 48,000 | 1.67배 |
| 16시간 / 2일 (8h×2) | 80,000 | 32,000 | 2.5배 |

> ⚠️ **`소정`근로시간이지 실근로시간이 아니다.** 연장근로는 주휴수당 산정에서 빠진다.
> 이 앱에서 가장 가까운 값은 예정 `end_time − start_time − rest`이며,
> `actual_*`이 아니다. 확정 정책 1(예정 시간대 기준 배분)과도 일관된다.

### 확정 정책 12 — `is_accepted` 백필 (D1 답변)

⚠️ **원래 질문(NULL 행)보다 `false` 행이 더 큰 문제다.**

```
WorkplaceJoinRequest:35  → is_accepted = false   (초대코드 참여는 무조건 false)
WorkerService:298        → true                  (사장님이 승인해야만)
```

`is_accepted`를 **읽는 코드가 0건**이었으므로, 사장님이 승인을 누르지 않아도
알바생은 아무 지장 없이 일해 왔다. 승인 버튼이 실제로 하는 일이 없었으니
**안 누른 사장님이 있을 수밖에 없다.**

→ **게이트를 켜는 순간 잘 쓰고 있던 사용자가 차단된다.** 이것이 Phase 2의 최대 배포 위험이다.

한편 NULL 행은 없을 가능성이 높다 — `is_accepted`는 모든 INSERT 경로에서
명시적으로 설정된다(`WorkplaceJoinRequest:35` false,
`OwnerWorkplaceCreateRequest:40` / `WorkerWorkplaceCreateRequest:46` true).

#### 백필 규칙 — NULL이든 `false`든 **근무 이력이 있으면 승인으로 본다**

```sql
-- 1. 규모 파악
SELECT is_accepted, COUNT(*) FROM workers GROUP BY is_accepted;

-- 2. 게이트를 켰을 때 잘못 차단될 사람 수 (0이면 그냥 켜면 된다)
SELECT COUNT(*) FROM workers w
 WHERE (w.is_accepted IS NULL OR w.is_accepted = 0)
   AND EXISTS (SELECT 1 FROM works k WHERE k.worker_id = w.id);

-- 3. 백필
UPDATE workers w SET is_accepted = 1
 WHERE (is_accepted IS NULL OR is_accepted = 0)
   AND EXISTS (SELECT 1 FROM works k WHERE k.worker_id = w.id);
UPDATE workers SET is_accepted = 0 WHERE is_accepted IS NULL;
```

**근거**: 근무 이력은 "실제로 일해 왔다"는 **관측 가능한 증거**이지 추측이 아니다.
잘못 판정되어도 **사장님이 승인/거부로 되돌릴 수 있다.** 반대 방향(전부 0)은
복구 경로가 사장님의 수동 조치뿐이고 그 전까지 사용자는 이유도 모른 채 막힌다.

진짜 미승인자(신청만 하고 일한 적 없는 사람)는 `0`으로 남는다 — 의도한 동작이며
확정 정책 4대로 근무지 이름·사장님 정보·승인 대기 표시만 보게 된다.

---

## 미결 — 남은 결정

| # | 질문 | 막고 있는 것 |
|---|---|---|
| ~~D2~~ | **앱에 없음이 확인됐다** → 관리자 공지 푸시가 아무에게도 안 간다. 서버에서 토큰 등록 시 구독시키는 방식이 앱 수정 없이 해결한다 (Phase 4에 편입). ~~앱에 `subscribeToTopic("ADMIN_ALARM")`이 있는가?~~ 서버에는 **0건**. 없으면 **관리자 공지 푸시가 아무에게도 안 간다**(FCM은 성공 반환, 인앱 목록에는 뜸). **토큰 기반 개별 알림 4곳과는 무관하며 그쪽은 정상 동작한다** | 7 I5 |
| **D3** | 루틴 알람 서버 발송 — 앱이 로컬 알림으로 처리 중인지 확인 필요. 아니라면 지금 아무도 못 받고 있다 → [10-2](#10-2-루틴-알람-서버-발송--d3-확인-대기) |
| ~~D4~~ | ~~루틴 완료 상태 보관~~ → **보관하기로 확정.** [10-1](#10-1-루틴-완료체크-상태-서버-보관--진행-결정-d4-답변) |
| ~~D5~~ | ~~근로소득세 구현 방식~~ → **a안 확정.** [정책 13](#확정-정책-13--근로소득세는-간이세액표-조회-d5-답변) |
| **D6** | 공제대상 가족 수 — 1명 고정(권장) vs 컬럼 추가해 입력받기 | Phase 3-8 |
| **입력 필요** | **홈택스에서 받은 근로소득 간이세액표 파일.** 법정 표라 임의 생성 불가 | Phase 3-8 |

## 지금 바로 착수 가능한 것

**Phase 0 → 1 → 2** 가 가장 값이 크다. Phase 0은 30분 규모이고,
Phase 1·2가 `is_accepted` 결함군(4개 스코프가 짚은 최다 확증 건)을 닫는다.

Phase 3-1~3-4, Phase 4 전체, Phase 8도 차단 요인이 없다.
