# 스코프 2 — 근무(Work) 도메인

- **범위**: `server/src/main/java/com/moup/domain/work/` 전체 2,600 LOC
- **기준 커밋**: `1172a1d`
- **판정**: 수정 후 병합 가능
- **테스트 현황**: Work 도메인 테스트 0건

## 잘 된 점

- **타임존 경계가 명확함.** API는 `Instant`(UTC), DB는 KST `LocalDateTime`으로
  고정하고 변환을 DTO/서비스 경계에서만 수행
  (`MyWorkCreateRequest.java:61-64`, `WorkService.java:241-244`, `1023-1024`).
  `TimeConstants.SEOUL_ZONE_ID` 단일 상수를 전 경로에서 쓰고
  `LocalDateTime.now()` 무인자 호출이 0건.
- **캘린더 N+1을 실제로 제거함.** `preloadCalendarWorkData`
  (`WorkService.java:1064-1077`) + `prefetchRepeatInfo` (`1080-1125`)가
  반복 그룹 정보를 그룹 수와 무관하게 쿼리 2회로 처리.
- **오버나이트 롤오버가 반복 생성에서 정확함.** `WorkService.java:806`의
  `dayOffset` → `813`의 `endTime.with(currentDate.plusDays(dayOffset))`.
- **야간 시간대 판정식이 정확함.** `SalaryCalculationService.java:156`의
  `isAfter(22:00) || equals(22:00) || isBefore(06:00)`가 `[22:00,24:00)`와
  `[00:00,06:00)` 양쪽을 커버.
- **동적 SQL 빈 컬렉션을 전부 가드함.** `WorkService.java:1087-1089`, `305`,
  `359`, `SalaryCalculationService.java:116`, `RoutineService.java:133-145`.
- **SQL Injection 없음.** `WorkRepository` 419줄에 `${}` 보간 0건. 삭제/수정
  SQL이 `WHERE id = ? AND worker_id = ?`로 소유자 조건을 함께 검
  (`WorkRepository.java:266`, `363`).

---

## Critical

### C1 — 음수 `restTimeMinutes`로 알바생이 자기 급여를 임의 증액 가능

| | |
|---|---|
| 수정 대상 | `domain/work/dto/MyWorkCreateRequest.java:32-34`, `MyWorkUpdateRequest.java:34-36` (스코프 2)<br>`domain/user/dto/WorkersWorkCreateRequest.java:33-35`, `WorkerWorkUpdateRequest.java:29-31` (**스코프 4**)<br>`SalaryCalculationService.java:164` (**스코프 3**)<br>`db/moup.sql:127` (**스코프 7**) |

4개 요청 DTO 모두 `@NotNull`만 있고 `@PositiveOrZero`가 없다. 계산부는 하한만 막는다:

```java
// SalaryCalculationService.java:164-165
long netWorkMinutes = grossWorkMinutes - restMinutes;
if (netWorkMinutes < 0) netWorkMinutes = 0;
```

**실패 시나리오**: `POST /workplaces/1/workers/me/works`에
`startTime=2025-11-10T01:00:00Z, endTime=2025-11-10T03:00:00Z, restTimeMinutes=-600`
→ gross 120분, **net 720분** → `basePay = 12시간 × 시급`. 스키마도
`rest_time_minutes INT`로 음수를 허용한다. 이 값은 `gross_income`을 거쳐
사장님 월별 정산에까지 반영된다.

**수정 방향**: 4개 DTO에 `@PositiveOrZero`, `verifyStartEndTime`에
`restTimeMinutes <= grossWorkMinutes` 검증, 스키마에 `CHECK (rest_time_minutes >= 0)`.

---

### C2 — 사장님이 알바생 근무 상세를 조회할 수 없음 (항상 404) ✅ 수정 완료

**develop 브랜치에 `219a8e4`로 커밋됨.** 사람이 보고한 파손이고 한 줄 수정이라
예외로 선반영했다.

`RoutineService.java:406`이 `findByIdAndUserId(work.getWorkerId(), userId)` →
`orElseThrow(WorkerNotFoundException)`이었다. 사장님의 `userId`로는 알바생의
`workers` 행을 못 찾아 404가 났고, `:412`의 사장님 허용 분기는 도달 불가능한
dead code였다. `findById`로 변경해 판정을 `:412`에 위임.

`view=SUMMARY`는 이 메서드를 호출하지 않아 기존에도 정상 동작했다 — 그래서
상세 화면만 깨져 있었다.

---

### C3 — 반복 근무 수정이 중복 근무를 생성 (데이터 손상)

| | |
|---|---|
| 수정 대상 | `domain/work/application/WorkService.java:873-892`, `794` (스코프 2) |

```java
// WorkService.java:880 — 기준 근무일 "이후"만 삭제
workRepository.deleteRecurringWorkFromDate(currentWork.getRepeatGroupId(), currentWork.getWorkDate());
// WorkService.java:889 → createRecurringWorks 내부
LocalDate startDate = startTime.toLocalDate();   // :794 — 요청의 startTime 기준
```

`newStartTime.toLocalDate() >= currentWork.getWorkDate()`를 강제하는 검증이 없다.

**실패 시나리오**: 10/06 시작 월·수 반복(종료 11/30) 생성 → 11/12(수) 근무를
열어 시간만 09:00→10:00으로 변경. 클라이언트가 `startTime`을 원본 그대로
`2025-10-06T00:00:00Z`로 보내면(반복 시작 시각이므로 충분히 가능한 UI 동작)
삭제는 11/12~11/30만, 생성은 10/06~11/30 전체 → **10/06~11/10의 모든 월·수에
근무 2건**. 주 근무시간이 2배로 집계되어 주휴수당 조건과 `gross_income`이
함께 틀어진다.

**수정 방향**: `replaceWithNewRecurringWorks` 진입 시 날짜 검증 후
`InvalidFieldFormatException`. 또는 삭제 기준일을
`min(currentWork.getWorkDate(), newStartDate)`로 맞춘다.

---

### C4 — 반복 삭제/교체 후 다른 주의 주휴수당이 stale (금액 오류)

| | |
|---|---|
| 수정 대상 | `WorkService.java:664-669`, `873-892`, `922`, `926-933` (스코프 2)<br>`SalaryCalculationService.java:87-88` (**스코프 3**) |

`recalculateWorkWeek`는 인자로 받은 `date`가 속한 **한 주만** 재계산한다.

```java
// WorkService.java:664-669 — 최대 365일치를 지우고 재계산은 1주만
long deletedCount = workRepository.deleteRecurringWorkFromDate(work.getRepeatGroupId(), work.getWorkDate());
salaryCalculationService.recalculateWorkWeek(context.worker().getId(), work.getWorkDate(), salary);
```

**실패 시나리오**: 알바생이 A매장에 월~금 반복(3개월)과 매주 토요일 단일 근무를
갖고 있다. 11/03(월) 기준 반복 삭제 → 11/10 이후 주는 주 15시간 미만이 됐는데
그 주 토요일 근무의 `holiday_allowance`/`gross_income`은 삭제 전 값 그대로 →
캘린더와 월 정산 과다 표시.

같은 결함이 세 곳:
- `replaceWithNewRecurringWorks` (`873-892`): 새 `repeatEndDate`가 짧아지면 그 사이 주 미재계산
- `deleteWorkHelper` (`926-933`), `updateSingleWorkInternal` (`922`): 근무일이 주 경계를 넘어 이동하면(금 → 다음 주 월) **원래 주** 미재계산

**수정 방향**: 삭제/이동 범위에 걸친 모든 주의 월요일을 `Set<LocalDate>`로 모아
루프 재계산. `createRecurringWorks`가 이미 `weeksToRecalculate`로 그렇게 한다
(`WorkService.java:804`, `825`, `835-837`).

---

### C5 — 출근 API가 3개의 분리된 트랜잭션, 실패 시 "퇴근 불가" 고착

| | |
|---|---|
| 수정 대상 | `domain/work/api/WorkController.java:229-248` (스코프 2)<br>`WorkerService.updateWorkerIsNowWorking` (**스코프 4**) |

```java
// WorkController.java:229-248 — 컨트롤러에 @Transactional 없음
if (workService.updateActualStartTime(userId, workplaceId)) { ... }   // tx 1
...
WorkCreateResponse response = workService.createMyWork(userId, workplaceId, request); // tx 2 (커밋됨)
workerService.updateWorkerIsNowWorking(userId, workplaceId, true);    // tx 3
```

**실패 시나리오**: `:247` 커밋 직후 `:248` 실패 → `actual_start_time`은 기록됐지만
`is_now_working=false`. 이후 퇴근 호출은 `WorkService.java:609`의
`if (!Boolean.TRUE.equals(userWorker.getIsNowWorking())) throw new WorkNotFoundException(...)`에
걸려 **영구 퇴근 불가**. 그 근무는 `actual_end_time IS NULL`이므로 다음 출근 시
`findEligibleWorkForClockIn`(`WorkRepository.java:151-152`)에서도 제외된다.
자가 복구 경로가 없다.

**수정 방향**: `WorkController.java:232-254`의 else 분기를 `WorkService`의 단일
`@Transactional` 메서드로 이동.

---

## Important

### I1 — 같은 매장 동료의 일별 추정 실수령액 노출

`WorkService.java:354-355`는 요청자가 그 매장의 worker이기만 하면 통과시킨다.
이후 `387-393`이 매장 **전 근무자**의 근무를 각자의 `salary`와 함께 변환하고,
`1027`의 `.estimatedNetIncome(isFixedSalary ? null : finalNetIncome)`이 그대로
응답에 실린다.

**시나리오**: 알바생 B가 `GET /workplaces/1/works?baseYearMonth=2025-11` →
알바생 A의 근무별 세후 일급이 전부 응답에 포함. 근무표 공유는 의도로 보이나
급여는 별개다.

**수정 방향**: `convertWorkToSummaryResponse` 호출부에서
`isMyWork == false && 요청자 != workplace.getOwnerId()`이면 `estimatedNetIncome`을 null로 마스킹.

### I2 — `is_accepted`(승인 대기)를 근무 도메인 어디서도 검사하지 않음

**스코프 4·5 교차.** `WorkplaceJoinRequest.java:35`가 `isAccepted(false)`로
worker 행을 만드는데, `WorkService`의 모든 진입점(`102`, `354`, `405`, `582`, `606`)이
`findByUserIdAndWorkplaceId`(`WorkerRepository.java:82`)만 쓰고 승인 여부를 보지 않는다.
grep 결과 `isAccepted`는 생성/업데이트 지점 외에 읽기 검증에서 **한 번도 쓰이지 않는다**.

**시나리오**: 초대 코드로 참여 요청만 하고 승인 전인 사용자가 즉시 근무 생성 /
출퇴근 / 매장 전체 캘린더 조회(= I1과 결합) 가능. 초대 코드가 필요하다는 점이
완화 요소라 Critical은 아니다.

### I3 — `PermissionVerifyUtil` NPE, 탈퇴 근무자 근무 접근 시 500

**스코프 7.** `PermissionVerifyUtil.java:10`의
`!workerUserId.equals(requesterUserId)` — `workers.user_id`는
`ON DELETE SET NULL`(`db/moup.sql:114`), `workplaces.owner_id`도 NULL 허용
(`db/moup.sql:95`). `WorkService.java:943`, `968`이 이 값을 그대로 넘긴다.
바로 아래 `WorkService.java:947`은 `getUserId() != null`을 체크한다 — 코드가
null 가능성을 아는데 `943`이 먼저 터진다.

**수정 방향**: `Objects.equals(...)`. `checkEditable`(`WorkService.java:1133-1135`)이
이미 null-safe하니 그 패턴으로 통일.

### I4 — 근무 중복/겹침 검사가 전혀 없음

`createSingleWork`(`751-780`), `createRecurringWorks`(`783-853`) 어디에도 시간 겹침
검사가 없고 스키마에도 유니크 제약이 없다(`db/moup.sql:118-141`).
`findAllByWorkerIdAndWorkDate`(`WorkRepository.java:105`)는 정의만 있고 호출자 0건.

**시나리오**: 같은 알바생에게 10:00~18:00과 12:00~20:00을 만들면 둘 다 저장.
겹친 6시간이 이중 계산되어 주 15시간 조건이 잘못 성립하고 `gross_income`이 중복 가산.

**수정 방향**: 범위 겹침은 UNIQUE로 못 막으므로 검사부터 넣고, 필요하면
worker 행 `SELECT ... FOR UPDATE`로 직렬화.

### I5 — 배치 생성의 부분 실패가 롤백되지 않고 응답과 어긋남

`WorkService.java:123`이 `@Transactional`인데 `171-199` 루프가 `catch (Exception e)`로
예외를 삼킨다. 근무자 2번이 `createBatch`(`833`) 성공 후 `recalculateWorkWeek`(`836`)에서
실패해도 이미 INSERT된 행은 같은 트랜잭션에 남아 커밋된다. 즉 **응답에 "실패"로
보고된 근무자에게 근무가 실제로 생성되어 있다.**

**수정 방향**: 근무자별 처리를 별도 빈의 `REQUIRES_NEW`로 분리하거나, 검증 실패만
catch하고 그 외는 전체 롤백.

### I6 — `night_work_minutes`가 야간수당 미설정자에게 항상 0으로 저장

**스코프 3.** `SalaryCalculationService.java:153`의 `if (hasNightAllowance) { ... nightWorkMinutes++; }`.
`night_work_minutes`는 "야간 근무시간(분)"이라는 **사실 데이터**다(`db/moup.sql:130`).
수당 지급 여부로 집계를 끄면 안 된다.

**시나리오**: 야간수당 미설정 알바생이 22:00~06:00 근무 → `night_work_minutes = 0`.
나중에 야간수당을 켜도 과거 데이터는 복구되지 않는다.

**수정 방향**: 시간 집계는 항상 수행, `hasNightAllowance`는 `:172`의 금액 계산에만 적용.

### I7 — 근무 시간 상한이 없어 분 단위 루프가 DoS 벡터

`verifyStartEndTime`(`WorkService.java:985-987`)은 `endTime.isBefore(startTime)`만 본다.
계산부는 1분씩 while 루프(`SalaryCalculationService.java:150-162`).

**시나리오**: `startTime=2020-01-01, endTime=2030-01-01`(둘 다 유효한 Instant) →
약 526만 회 루프. 이어지는 `recalculateWorkWeek`가 그 주 전체를 다시 돌고,
반복 생성이면 근무 건수만큼 배가되며 그동안 트랜잭션이 점유된다.

**수정 방향**: `Duration.between(start, end).toHours() <= 24` 검증. 계산도 루프 대신
구간 교차 산술로 O(1) 처리 가능.

### I8 — 주휴수당 배분의 분모/분자 불일치로 금액 누락

**스코프 3.**

```java
// SalaryCalculationService.java:108 — 분모는 endTime null 근무 포함
int dailyHolidayAllowance = weekWorks.isEmpty() ? 0 : weeklyHolidayAllowance / weekWorks.size();
// :110-113 — 적용 대상은 endTime != null 인 근무만
```

주 5건 중 1건이 퇴근 미기록(진행 중)이면 주휴수당을 5로 나눠 4건에만 지급 →
20% 누락. 정수 나눗셈 나머지도 버려진다.

추가로 `:93-96`의 `weeklyWorkMinutes`는 clamp가 없어 C1의 음수 rest가 주 합계를
**감소**시킨다 — `:164-165`의 clamp와 동작이 불일치한다.

### I9 — 반복 생성 시 루틴 매핑이 근무 건수만큼 반복 쿼리 (쓰기 N+1)

**스코프 6.** `WorkService.java:846-850`의 루프가 근무당
`saveWorkRoutineMapping`을 호출하고, 이는 DELETE(`RoutineService.java:371`) +
SELECT(`:379`) + 배치 INSERT = 3쿼리다. 365일 주5일 반복이면 약 260건 × 3 ≈
**780쿼리가 한 트랜잭션 안에서** 실행된다.

**수정 방향**: 루틴 유효성 검증을 1회만 하고 전체 매핑을
`WorkRoutineMappingRepository.createBatch` 한 번으로 삽입.

### I10 — Owner의 삭제 권한이 수정 권한과 불일치, 경로 우회 가능 ⚠️ 의도 확인 필요

`updateMySingleWork`(`WorkService.java:438-440`)와 `updateMyRecurringWork`(`469-471`)는
"본인 근무만"을 명시적으로 강제한다. 반면 `deleteWork`(`648-653`)와
`deleteRecurringWorkIncludingDate`(`657-670`)는 `getVerifiedWorkContextForUD`(`962-971`)만
통과하면 되고, 이는 **매장 사장님도 허용**한다.

결과: 사장님은 `DELETE /works/{workId}`로 알바생 근무를 지울 수 있지만
`PATCH /works/{workId}`로는 403이다. 사장님용 수정 엔드포인트는
`workplaceId`/`workerId`를 경로에 요구하는데(`WorkController.java:165`, `180`)
삭제만 `workId` 하나로 그 스코핑을 우회한다.
`WorkSpecification.java:266-278`, `280-292`는 이 차이를 문서화하지 않는다.

### I11 — 1,137줄 서비스의 책임 분리 (두 덩어리만)

- **반복 근무 로직** (`783-892`, 약 110줄): `createRecurringWorks` /
  `replaceWithNewRecurringWorks` / `stopRecurrenceAndUpdateSingle`이 서로 다른
  삭제 기준일(`deleteRecurringWorkFromDate`의 `>=` vs `deleteRecurringWorkAfterDate`의 `>`)을
  쓰는데 그 차이가 한 파일에 흩어져 있어 C3/C4 같은 버그가 눈에 안 띈다.
  `RecurringWorkManager`로 묶으면 세 경로의 경계 조건을 나란히 볼 수 있다.
- **응답 조립** (`937-959`, `974-982`, `990-1033`, `1036-1050`, `1080-1125`, 약 190줄):
  순수 변환 로직. `WorkResponseAssembler`로 옮기면 서비스가 800줄대로 내려간다.

이 둘 외의 추가 분리는 지금 불필요.

---

## Minor

| # | 내용 |
|---|---|
| M1 | **Dead code.** 호출자 0건: `stopRecurrenceAndUpdateSingle`(`WorkService.java:856-870`), `WorkRepository.findAllByWorkerIdAndWorkDate`(`:105`), `findFirstWorkByRepeatGroupId`(`:182`), `CalendarWorkData.allWorks`(`:85`). `toEntity` 4개(`MyWorkCreateRequest:44-78`, `MyWorkUpdateRequest:46-81`, `WorkersWorkCreateRequest`, `WorkerWorkUpdateRequest`) 전부 미사용 — 서비스가 `calculateDailyIncome` 결과의 `toBuilder()`로 직접 조립한다(`765-773`, `910-918`). 이 미사용 `toEntity`들이 `grossIncome = base + night + holiday` 규칙을 **4벌 중복 보유**하고 있어 계산 규칙이 바뀌면 서로 어긋난다. |
| M2 | **Spec-구현 경로 불일치.** `WorkSpecification.java:179`는 `@GetMapping("/{workplaceId}/works")`, `WorkController.java:125`는 `"/workplaces/{workplaceId}/works"`. 라우팅은 구현 클래스가 이기므로 동작 영향은 없으나 인터페이스 문서가 틀렸다. |
| M3 | **Spec-구현 계약 불일치.** `endTime`이 `MyWorkCreateRequest.java:27-29`에서 `@NotNull` + `REQUIRED`인데 `WorkController.java:239`가 출근 API에서 `endTime(null)`로 빌더를 호출해 Bean Validation을 우회한다. 실제 도메인은 nullable(`db/moup.sql:123`)인데 API 문서만 필수라고 말한다. |
| M4 | **0분 근무 허용.** `verifyStartEndTime`(`985-987`)이 `isBefore`만 보므로 `endTime == startTime` 통과. `!isAfter`로 변경. |
| M5 | **죽은 분기.** `WorkService.java:620`의 `!Objects.equals(workToEnd.getEndTime(), currentDateTime)` — 나노초 정밀도 `now()`와의 비교라 사실상 항상 true. 동작은 맞으나 조건문이 무의미. |
| M6 | **출근 check-then-act 경합.** `:585`의 `existsByUserIdAndIsNowWorking` → `592-595`의 갱신 사이에 락이 없다. `updateIsNowWorking`의 UPDATE에 `AND is_now_working = false`를 넣고 갱신 행 수로 판정하면 한 줄로 해결. |
| M7 | **복합 인덱스 부재.** `works`에 FK 인덱스(`worker_id`)와 `idx_repeat_group_id`만 있다(`db/moup.sql:139-141`). 실제 접근 패턴은 거의 전부 `worker_id` + `work_date` 범위(`WorkRepository.java:114`, `123-133`, `240-250`, `286-296`, `300-311`). `(worker_id, work_date)` 복합 인덱스면 13개월 캘린더 조회의 filesort가 사라진다. |
| M8 | **`SELECT *` 광범위 사용** (`WorkRepository.java:17`, `82`, `90`, `97`, `105`, `114`, `125`, `149`, `169`, `182`). `map-underscore-to-camel-case=true`라 동작하지만 컬럼 변경에 결합된다. |
| M9 | **캘린더 응답 상한 없음.** `getAllMyWork`(`300-301`)은 13개월 × 전 매장 근무를 한 번에 반환. 페이지네이션·건수 캡 없음. 매장 5곳 × 13개월 일근무면 2천 건 규모 응답. |
| M10 | **(스코프 6) off-by-one.** `RoutineService.java:365`의 `if (routineIdList.size() >= MAX_ROUTINE_COUNT_PER_WORK)` — 최대 개수 자체가 거부된다. `>`여야 한다. |
| M11 | **(스코프 3) 주휴수당 시급 산정.** `SalaryCalculationService.java:103`이 `weekWorks.get(0).getHourlyRate()`를 쓴다. 주 중 시급이 바뀌면 첫 근무 시급이 주 전체에 적용되고, `hourly_rate INT NULL`(`db/moup.sql:132`)이므로 레거시 행에서 언박싱 NPE가 난다. |

---

## 미확인 — 확인 필요

**JDBC 커넥터 타임존 옵션.** `spring.datasource.url=${DATABASE_URL}`이라 실제
URL을 확인하지 못했다. `LocalDateTime` ↔ `DATETIME`은 Connector/J 기본 설정에서
TZ 변환이 없어 현재 설계가 안전하지만, URL에
`connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` 류가 붙어 있으면
저장 값이 9시간 밀린다.

> **질문**: 운영 `DATABASE_URL`에 `connectionTimeZone` / `serverTimezone` /
> `preserveInstants` 파라미터가 붙어 있는가?

---

## 테스트 우선순위

Work 도메인 테스트는 현재 0건이다. 1~5번만 붙여도 Critical 4건이 회귀로 고정된다.

| # | 대상 | 입력 | 기대 | 현재 |
|---|---|---|---|---|
| 1 | `calculateDailyIncome` 음수 휴게 | `start=2025-11-10T10:00, end=12:00, rest=-600` | `net=120` (또는 요청 거부) | `net=720` |
| 1b | 과대 휴게 | 위와 동일, `rest=600` | `net=0` | — |
| 2 | 반복 수정 중복 생성 | 10/06 시작 MON/WED 반복(end 11/30) 생성 → 11/12 근무 ID로 `startTime`을 10/06 유지한 채 `PATCH /works/recurring/{id}` | 10/06~11/10 각 월·수 근무 **1건** | 2건 |
| 3a | 오버나이트 | `start=11-10T22:00, end=11-11T06:00, rest=0, hasNight=true` | `gross=480, night=480` | — |
| 3b | 부분 겹침 | `20:00~23:00` | `night=60` | — |
| 3c | 두 야간 창 교차 | `05:00~23:00` | `night=120` | — |
| 4 | 반복 삭제 후 타 주 주휴수당 | 월~금 반복 8주 + 매주 토 단일 근무 → 3주차 월요일 기준 반복 삭제 | 4~8주차 토요일 `holiday_allowance = 0` | 이전 값 유지 |
| 5 | 사장님 근무 상세 조회 | 사장 계정으로 `GET /works/{알바생 근무 ID}` (view 기본값) | 200 | ~~404~~ → C2 수정 완료 |
| 6 | 동료 급여 노출 | 알바생 B 계정으로 `GET /workplaces/{id}/works` | A 항목의 `estimatedNetIncome`이 null | 값 노출 |
| 7 | 탈퇴 근무자 NPE | `workers.user_id`를 NULL로 만든 뒤 사장 계정으로 `GET /works/{id}` | 200 + nickname `"탈퇴한 근무자"` | 500 |
| 8 | 배치 생성 원자성 | workerId 3개 중 2번째가 예외를 던지게 함 | `failedWorkerInfoList`와 DB 실제 행 수 일치 | 불일치 |
| 9 | 승인 대기 근무자 차단 | `is_accepted=false`로 `POST /workplaces/{id}/workers/me/works` | 403 | 통과됨 |
| 10 | 반복 기간 상한 회귀 방지 | `repeatEndDate = startDate + 366` | `DATA_LIMIT_400` | (정상 동작 확인용) |

---

## 총평

타임존 경계 처리, 캘린더 N+1 제거, 오버나이트 롤오버, SQL Injection 부재는 이
규모 코드베이스에서 기대 이상이다. 다만 C1(음수 휴게시간으로 인한 급여 조작),
C3(반복 수정 시 근무 중복 생성), C4(삭제 후 주휴수당 stale)는 **금전 데이터가
틀어지는 버그**이고, C2(사장님 상세 조회 404)는 기능 파손이었다(수정 완료).
C5와 I2·I3는 각각 복구 불가 상태 고착과 500/무권한 접근이라 함께 처리를 권한다.
