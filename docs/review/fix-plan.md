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

## Phase 1 — 스키마 제약 일괄 (마이그레이션 1회)

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

## Phase 2 — `is_accepted` 승인 게이트 (5 C-1 대표)

**코드베이스 전체에서 `is_accepted`를 읽는 곳이 0건**이다. 승인 절차가 장식이다.

Phase 1(스키마) 완료가 전제. 영향 파일이 가장 넓다:
`WorkService` · `WorkerService` · `WorkplaceService` · `RoutineService` · `SalaryService`

**확정 정책 4**: 승인 대기 중에는 (a) 근무지 이름, (b) 사장님 정보, (d) 승인 대기 표시만 보인다.
**확정 정책 7**: 미승인자의 근무에는 루틴을 연결할 수 없다.

이걸 고치면 **4 I5 / 2 I1(동료 급여 노출)의 절반이 함께 닫힌다** — 미승인자의 접근이 먼저 막힌다.

---

## Phase 3 — 급여 계산 (스코프 3 중심)

한 파일(`SalaryCalculationService`, 705줄)에 집중된다. **내부 순서 의존이 강하다.**

```
3-1  C-5  night_work_minutes가 휴게시간 미제외 · dayTimeMinutes 음수
     M-8  주간 합계 음수 클램프 부재       ← 두 건이 같은 계산의 앞뒤다
3-2  C-4  night_work_minutes(사실)가 수당 지급 여부에 종속 — 기록과 지급 분리
3-3  M-1  부동소수점 절삭 (90,310건 불일치, 항상 근로자에게 불리)
     M-2  주휴수당 배분 나머지 버림
3-4  C-2  주휴수당 분모/분자 집합 통일 + I-2 hourly_rate NULL
3-5  C-1  주휴수당 산식 자체        ← 정책 11로 확정
3-6  C-6  재계산 범위 (+ 2 C4)     ← C-1 확정 후에 해야 기대값이 안 흔들린다
3-7  C-7  수당 플래그가 스냅샷 아닌 현재값을 읽음 (확정 정책 3 위반)
     C-3  이미 호출 제거로 확정
```

**차단 해소됨** — Q9는 [정책 11](#확정-정책-11--주휴수당-법정-산식-q9-답변),
Q5는 [정책 10](#확정-정책-10--알바생-소득은-근로소득-q5-답변)으로 확정됐다.
**Phase 3 전체가 착수 가능하다.** 단 세금(I-6 → Critical 격상)은 D5 선택이 남았다.

```
3-8  I-6  근로소득 과세로 전환 (현재 사업소득 3.3% + 지방세 10% = 3.63%)
          정책 13(간이세액표 조회)으로 방식 확정. 표 파일 + D6 결정이 남았다.
```

---

## Phase 4 — FCM 재설계 (6 C1·C2·C3 + 정책 8·9)

```
4-1  fcm_tokens(user_id, token, device_id, updated_at) 테이블 분리   ← 정책 8
     UNIQUE (token) → C2(오배달) 자동 해소
     logout이 기기별 행만 삭제 → "폰 로그아웃하면 태블릿도 죽는" 문제 해소
4-2  FCMTokenService에 null/blank 가드 한 줄                          ← C3
4-3  sendToSingleUser에서 @Transactional 제거 · afterCommit 전송      ← C1 1단계
     호출부 3곳의 catch→throw 제거 (푸시는 best-effort)               ← C1 2단계, 정책 9
     FCMService:45의 미사용 sender 조회 삭제 → 탈퇴자 관리 차단 해소   ← C1 4단계, 정책 9/Q16
4-4  UNREGISTERED/INVALID_ARGUMENT/SENDER_ID_MISMATCH 시 토큰 행 삭제  ← C1 3단계
4-5  sendEachForMulticast 전환
4-6  I2 공지 발송 순서 (커밋 전 푸시 · @Async가 미커밋 FK 참조)
4-7  토큰 등록 시 서버에서 ADMIN_ALARM 토픽 구독              ← D2 확인 결과
     앱에 subscribeToTopic이 없어 지금 공지 푸시가 0명에게 간다.
     서버 측 subscribeToTopic(tokens, topic)이면 앱 수정 없이 해결된다.
```

**4-1이 4-4보다 먼저**여야 한다 — 단일 컬럼 구조에서는 "죽은 토큰만 삭제"가 불가능하다.

---

## Phase 5 — 반복 근무 (2 C3 · C4 · C5)

- **C3** 반복 근무 수정이 중복 근무 생성 (데이터 손상)
- **C4** 반복 삭제/교체 후 다른 주의 주휴수당 stale → **Phase 3-6과 같은 뿌리**
- **C5** 출근 API 3개 분리 트랜잭션 → "퇴근 불가" 고착

C4는 Phase 3-6과 함께 처리한다.

---

## Phase 6 — 사장님 탈퇴 시 가명처리 (4 C4 · 확정 정책 5)

`withdrawn_at` 컬럼 추가 + 접근 차단 로직. **Phase 0-1 필수 전제.**
FK는 이미 `SET NULL`로 고쳤으므로(`98ac8e9`) 데이터 소실은 멈췄고,
남은 것은 "남은 데이터를 어떻게 보여줄 것인가"다.

---

## Phase 7 — 레이트 리밋 (5 C-3 · 7 I17)

초대코드 상환·`/auth/**`에 레이트 리밋이 코드에도 nginx에도 없다.
**Phase 2 완료 후**에 하는 게 낫다 — C-3의 위험도는 C-1(`is_accepted`)과 연쇄해서 커진다.

---

## Phase 8 — 검증·에러 처리 잔여 (Important 다수)

독립적이라 언제든 가능. 묶어서 한 번에:

| 항목 | 출처 |
|---|---|
| checked 예외가 핸들러를 통과해 Boot 기본 `/error`로 나감 | 7 I6 |
| `ErrorCode.INVALID_TOKEN`이 401이 아니라 400 | 7 I16 |
| CORS 두 곳에 다르게 정의 · Security 쪽에 PATCH 누락 | 7 I12 |
| `PATCH`가 전체 치환으로 동작해 주소·좌표를 지움 | 5 I-1 |
| DTO 검증이 스키마 제약 미반영 (422여야 할 것이 500) | 5 I-3 · 3 I-3 · I-4 · 4 I9 |
| 빈 알림함에 404 | 6 I3 |
| 파일 업로드가 클라이언트 `Content-Type`만 신뢰 | 7 I7 |
| 프로필 이미지 교체 시 삭제가 업로드보다 먼저 | 7 I8 · 4 I8 |
| refresh token 평문 저장 | 1 I7 |
| 인증 실패가 401이 아니라 500 | 1 I3(b) |

---

## Phase 9 — 인프라 운영 (코드 아님)

| 항목 | 출처 | 성격 |
|---|---|---|
| 🔴 **Firebase 키 폐기·재발급** | 7 C7 | **지금 당장.** 코드 아님 |
| `manual-db-init.yml`이 확인 절차 없이 프로덕션 DB를 날림 | 7 C4 | 워크플로 수정 |
| 배치 실행 경로가 버전 관리 밖 | 7 C5 · INF-1·2 | `delete_old_users.sh`를 리포지토리로 |
| 프로덕션이 develop 이미지를 가져감 | 7 C6 | 태그 분리 |
| 프로파일 분리 부재 · Docker 로그 로테이션 없음 | 7 I2 | SD 카드가 찰 때까지 자란다 |
| `/health`가 DB·Redis 미확인 | 7 I13 | `/actuator/health`로 대체 |

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
