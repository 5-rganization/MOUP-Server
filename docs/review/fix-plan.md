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

## 🔴 Phase 0 — 이걸 먼저 안 하면 뒤가 전부 위험하다

### 0-1. `@NoArgsConstructor` 부재 (4 I6 · 5 I-9 · 6 M9)

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

**작업**: 엔티티 9개에 `@NoArgsConstructor` + `@Setter`(또는
`arg-name-based-constructor-auto-mapping=true` 한 줄). **후자가 더 짧다** —
전역 설정 한 줄로 9개 클래스를 안 건드린다. 다만 파라미터 이름이 유지되도록
`-parameters` 컴파일 옵션 확인이 필요하다.

**검증**: 컬럼 순서를 일부러 뒤섞은 `SELECT`로 매핑 테스트 1개.

### 0-2. 음수 `restTimeMinutes` 하한 (2 C1 · 3 C-5 · 4 C5)

DTO 6곳에 `@PositiveOrZero`. **급여 계산을 손대기 전에** 해야 한다 — 안 그러면
Phase 3의 회귀 테스트가 음수 입력을 정상으로 가정한 기대값에 고정된다.

### 0-3. 상한 off-by-one 3건 (6 I8)

`>=` → `>`. `RoutineService:82`, `:333`, `:365`. 한 글자씩 3곳.
Phase 0에 넣는 이유는 **다른 것과 충돌하지 않고 지금 안 하면 잊혀서**다.

---

## Phase 1 — 스키마 제약 일괄 (마이그레이션 1회)

Phase 0-1 완료가 **전제**다. 흩어서 하면 운영 DB에 `ALTER`를 여러 번 치게 되니 묶는다.

| 대상 | 근거 | 내용 |
|---|---|---|
| `workers` | 5 C-2 · 7 I11 | `UNIQUE (workplace_id, user_id)` — 중복 참여 경합 차단 |
| `workers.is_accepted` | 5 C-1 | `NOT NULL DEFAULT 0` + 기존 NULL 행 정리 |
| `works` | 7 I10 · 2 M7 | `INDEX (worker_id, work_date)` — 13개월 캘린더 filesort 제거 |
| `normal_alarms` | 6 I5 · 7 I9 | `INDEX (receiver_id, sent_at DESC)` + `receiver_id`/`sender_id` FK CASCADE |
| `routines` | 6 M5 | `UNIQUE (user_id, routine_name)` |
| `workplaces` | 5 I-7 | `UNIQUE (owner_id, workplace_name)` |
| `admin_alarm_user_mappings` | 6 M12 | `UNIQUE (alarm_id, user_id)` |
| `salaries` | 7 I14 | `CHECK` — `HOURLY`인데 `hourly_rate IS NULL` 방지 |

⚠️ **`is_accepted` 기존 NULL 행을 무엇으로 채울지 결정이 필요하다.**
전부 `1`(승인됨)로 치면 미승인자가 승인되고, `0`으로 치면 현재 정상 근무 중인
사람이 차단된다. → [미결 D1](#미결---제품-소유자-결정-필요)

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
3-5  C-1  주휴수당 산식 자체        ⛔ Q9 답변 없이는 착수 불가
3-6  C-6  재계산 범위 (+ 2 C4)     ← C-1 확정 후에 해야 기대값이 안 흔들린다
3-7  C-7  수당 플래그가 스냅샷 아닌 현재값을 읽음 (확정 정책 3 위반)
     C-3  이미 호출 제거로 확정
```

**⛔ 3-5와 3-6은 Q9(주휴수당 법정 산식)가 막고 있다.** 3-1~3-4는 지금 진행 가능하다.
**I-6(3.3% 이중 공제)은 Q5가 막고 있다.**

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

## 미결 — 제품 소유자 결정 필요

| # | 질문 | 막고 있는 것 |
|---|---|---|
| **Q5** | 알바생 소득이 **근로소득**인가 **사업소득**인가? 현재 3.3%에 지방소득세 10%를 다시 더해 이중 공제 중 | 3 I-6 |
| **Q9** | 주휴수당 **법정 산식** 확정 (현재 산식은 주 5일 미만 근무자에게 최대 2.5배 과다 지급) | Phase 3-5, 3-6 |
| **D1** | `is_accepted` 기존 NULL 행을 `0`으로 채우나 `1`로 채우나? | Phase 1 |
| **D2** | 클라이언트가 `ADMIN_ALARM` 토픽을 구독하는가? (서버에 `subscribeToTopic` **0건**) | 7 I5 |
| **D3** | 루틴 알람(`routines.alarm_time`)을 서버가 발송해야 하는가? (`@Scheduled` **0건**) | 6 미확인2 |
| **D4** | 루틴 완료(체크) 상태를 서버가 보관해야 하는가? (관련 컬럼 **0건**) | 6 미확인3 |

## 지금 바로 착수 가능한 것

**Phase 0 → 1 → 2** 가 가장 값이 크다. Phase 0은 30분 규모이고,
Phase 1·2가 `is_accepted` 결함군(4개 스코프가 짚은 최다 확증 건)을 닫는다.

Phase 3-1~3-4, Phase 4 전체, Phase 8도 차단 요인이 없다.
