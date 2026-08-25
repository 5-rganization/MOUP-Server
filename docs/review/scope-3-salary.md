# 스코프 3 — 급여 계산(Salary) 도메인

- **범위**: `server/src/main/java/com/moup/domain/salary/` 전체 1,059 LOC
- **판정**: **아니오 — 수정 후** (스코프 2보다 엄격)
- **집계**: Critical 6 / Important 11 / Minor 8 / 확인 질문 8
- **테스트 현황**: 0건
- **리뷰 격리**: 리뷰어에게 `docs/review/`를 열지 말라고 지시했고, 확정 정책 2건만
  전제로 주었다. 아래 "교차 검증" 절의 항목들은 스코프 2 결과를 **모르는 상태에서**
  독립적으로 도달한 결론이다.

---

## 교차 검증 — 스코프 2와의 대조

### 독립 확증 (두 리뷰어가 서로 모른 채 같은 지점을 짚음)

| 스코프 3 | 스코프 2 | 판정 |
|---|---|---|
| C-2 주휴수당 분자/분모 집합 불일치 | I8 | **확증.** 스코프 3이 구체적 금액(24,520원 증발)까지 계산 |
| C-4 `night_work_minutes`가 수당 여부에 종속 | I6 | **확증.** 스코프 3이 `dayTimeMinutes` 왜곡까지 추적 |
| C-6 재계산 범위 < 변경 범위 | C4 | **확증 + 확장.** 스코프 3이 월 경계 케이스 (c)를 추가 발견 |
| I-10 근무 시간 상한 없음 (분 단위 루프 DoS) | I7 | **확증** |
| I-2 `hourly_rate` NULL 언박싱 NPE + `weekWorks.get(0)` | M11 | **확증 + 심각도 상향** (Minor → Important) |
| M-8 주간 합계에 음수 클램프 없음 | C1 부분 | **확증** |
| I-1 실제 퇴근이 급여에 미반영 | M5 (죽은 분기) | **진단 심화.** 스코프 2는 "무의미한 조건문"으로 봤으나, 실제로는 조퇴/연장이 급여에 1원도 반영되지 않는 **기능 결함** (Minor → Important) |

### 스코프 2가 놓친 것 — 부분 시야의 한계 실증

**C-1 주휴수당 산식 자체가 법정 산식이 아니다.** 스코프 2는 경계에서만 봤기에
분모 불일치(I8)까지만 발견했다. 스코프 3이 `SalaryCalculationService` 전체를 보고
**산식 자체가 틀렸음**을 찾았다.

> **이것이 "리뷰 도중 수정하지 않는다"는 결정의 직접적 근거다.**
> 스코프 2의 I8 수정(분모를 필터된 건수로 통일)을 그때 적용했다면,
> **근본적으로 틀린 산식의 분모만 고친 상태**가 되어 여전히 최대 2.5배
> 과다 지급이 남았을 것이고, 테스트까지 그 잘못된 기대값으로 고정됐을 것이다.

그 외 스코프 3 단독 발견: C-3, C-5, I-3, I-4, I-5, I-6, I-7, I-8, I-9, I-11,
M-1 ~ M-7.

---

## 확정 정책 3 — 급여 스냅샷 (Q1 답변)

> **이미 등록된 근무의 급여는 등록 시점 스냅샷을 유지한다. 미래 예정 근무와
> 과거 근무 모두 해당한다.** 급여 설정을 바꿔도 기존 근무 행은 손대지 않는다.

### 이미 정책에 맞는 부분

`SalaryCalculationService.java:167`
```java
int hourlyRate = (work.getHourlyRate() != null) ? work.getHourlyRate() : 0;
```
일급 계산의 시급은 **근무 행의 스냅샷**을 쓰고 null-safe하다. 정책대로다.

### C-3 수정 방향 확정 — 호출을 제거한다

`WorkerService.java:197-222`, `:230-253`, `WorkplaceService.java:219-243`의
`recalculateEstimatedNetIncomeForMonth` 호출은 **스냅샷 정책 위반이므로 삭제**한다.
이 호출은 새 공제 설정을 옛 `gross_income`에 적용해 `estimated_net_income`만
갱신하는데, 스냅샷 정책에서는 급여 설정 변경이 기존 근무를 건드리면 안 된다.
(C-3에 기술된 "절반만 재계산해 데이터가 모순되는 상태"는 호출을 지우면 해소된다.)

### I-2 수정 권고 정정 ⚠️

리뷰어는 I-2에서 주휴수당 기준 시급을 `weekWorks.get(0)` 대신
**`salary.getHourlyRate()`(현재값)로 바꾸라**고 했다. **스냅샷 정책에서는 틀린
방향이다.** 현재값을 쓰면 급여 설정 변경이 과거 주휴수당에 소급 적용된다.

**올바른 수정**: 근무 행의 스냅샷 시급을 유지하되 ① null-safe하게 만들고
② `get(0)`(=`ORDER BY work_date`의 가장 이른 건)이라는 임의 선택을 없앤다.
주 중간에 시급이 바뀌어 스냅샷이 섞이는 경우의 기준을 명시적으로 정할 것 — Q10.

### C-7 (신규) — 수당 플래그는 스냅샷이 아니라 현재값을 읽는다

`SalaryCalculationService.java:80-85`
```java
if (salary != null && salary.getSalaryCalculation() == SALARY_CALCULATION_FIXED) return;
boolean hasHolidayAllowance = (salary != null) && salary.getHasHolidayAllowance();
boolean hasNightAllowance   = (salary != null) && salary.getHasNightAllowance();
```

`hourly_rate`와 달리 `has_holiday_allowance` / `has_night_allowance` /
`salary_calculation`은 **`works`에 스냅샷 컬럼이 없다**(`db/moup.sql:118-141`).
게다가 `salaries`는 이력이 없다 — `SalaryRepository.java:54-60`이 제자리 UPDATE라
옛 설정은 영구 소실된다.

**실패 시나리오**: 야간수당 없이 근무 10건 등록(각 `night_allowance = 0`) →
나중에 야간수당을 켬 → 그 주의 **아무 근무나 하나 수정** → `recalculateWorkWeek`이
그 주 전체를 **새 플래그로** 재계산. 결과적으로 건드린 주만 새 정책, 나머지 주는
옛 값이 되어 **같은 알바생의 근무가 주마다 다른 정책으로 계산된다.** 어느 주가
어느 정책인지는 "수정을 건드렸는지"라는 무관한 이력에 좌우된다.

**수정 방향 (택1)**:
- (a) `works`에 `has_night_allowance` / `has_holiday_allowance` 스냅샷 컬럼 추가 —
  정책에 정확히 부합하나 마이그레이션 필요
- (b) `salaries`에 이력(유효기간)을 도입하고 근무일 기준으로 조회 — 더 크지만
  Q5(소득 유형)·Q4(최저임금 이력)까지 함께 풀린다
- (c) 재계산 시 수당 금액을 근무 행의 기존 값에서 역산 — 가장 작은 diff지만
  `night_allowance = 0`이 "수당 없음"인지 "야간 근무 0분"인지 구분이 안 되어 취약

**(a)를 권한다.** C-4·C-5 수정으로 `night_work_minutes`가 사실 기록이 되면
(c)의 모호성도 사라지지만, 플래그 스냅샷이 없으면 여전히 소급 적용을 막을 수 없다.

---

## 확정 정책 4 — 주휴수당 산식·기준 시급·소급 적용 (Q9/Q10/Q11 답변)

### Q9 → (a) 법정 산식 채택

```java
double weeklyHours = Math.min(weeklyWorkMinutes / 60.0, 40.0);
weeklyHolidayAllowance = (int) Math.round(weeklyHours / 40.0 * 8.0 * hourlyRate);
```

산식 선택과 무관하게 현재 코드는 폐기 대상이다 — **주 20시간을 며칠에 나눠
일했느냐로 주휴수당이 2.5배 달라진다**(2일×10h → 100,300원 / 5일×4h → 40,120원,
시급 10,030 기준). 총 근로시간이 같은데 근무일수 쪼개기가 금액을 바꾸는 것은
어떤 정책으로도 정당화되지 않는다.

⚠️ 법정 산식의 **정확한 형태와 개근 요건(Q3)** 은 노무 확인이 필요하다.
코드가 자기모순이라는 점만 확언 가능하다.

### Q10 → **그 주 마지막 근무의 시급** (가중평균에서 변경)

한 주 안에 스냅샷 시급이 섞일 때의 주휴수당 기준 시급은 **그 주 마지막 근무의
시급**으로 한다. `weekWorks.get(0)`(= `ORDER BY work_date`의 가장 이른 근무,
`WorkRepository.java:114`)은 폐기한다.

> **변경 이력**: 처음에는 가중평균으로 정했으나, 주휴수당의 통상임금 기준을
> **"주휴일이 속한 시점"** 으로 보는 해석에 맞춰 마지막 근무 기준으로 변경했다.
> 가중평균은 시급 인상 주에서 인상 후 시급보다 낮아 **근로자에게 불리한
> 방향**이었다.

**구현** — `payableWorks`(= `end_time != null` 필터, C-2와 공유)의 뒤에서부터
첫 non-null 시급:

```java
int hourlyRate = 0;
for (int i = payableWorks.size() - 1; i >= 0; i--) {
    Integer r = payableWorks.get(i).getHourlyRate();
    if (r != null) { hourlyRate = r; break; }
}
```

**선행 조건 — 가중평균 대비 크게 줄었다:**

| 선행 | 가중평균이었다면 | 마지막 근무 기준 |
|---|---|---|
| **C-2** (payableWorks 통일) | 필수 | **여전히 필수.** `weekWorks` 전체의 마지막을 쓰면 `end_time = NULL`인 근무가 기준이 될 수 있다 |
| **I-2** (`hourly_rate` NULL) | **필수 · 영향 확대** (NULL 1건이 항상 전체 평균을 끌어내림) | **완화.** 노출이 다시 1/n로 줄고, 위 코드처럼 뒤에서부터 첫 non-null을 찾으면 해소된다 |
| **M-8** (음수 net 클램프) | 필수 (가중치가 음수가 됨) | **무관.** 기준 시급 선정에 net을 안 쓴다 (단 주 근로시간 합계 때문에 M-8 자체는 여전히 수정 대상) |

**새로 생기는 요구사항 — 정렬 tie-break (필수)**

`WorkRepository.java:114`의 `ORDER BY work_date`는 **같은 날 여러 근무**의 순서를
보장하지 않는다. I-4에서 확인했듯 근무 중복 검사가 없어 같은 날 2건 이상이
가능하고, 그 둘의 시급이 다르면 **어느 쪽이 "마지막"인지 비결정적**이다.

→ `ORDER BY work_date, start_time, id`로 tie-break를 명시할 것.
(`get(0)` 방식에서도 잠재해 있던 문제가 반대편으로 옮겨온 것이다.)

**허용되는 부작용**: 그 주 마지막 근무를 삭제하면 기준 시급이 그 앞 근무로
점프한다. 다만 "주휴일 시점 통상임금" 관점에서는 **남은 근무 중 마지막이 기준이
되는 것이 논리적으로 일관**되므로 수용한다.

**설명 가능성 (가중평균 대비 개선)**: UI에 "10/13 근무 시급 12,000원 기준"처럼
실재하는 근무 하나를 지목할 수 있다. 가중평균의 "평균 시급 11,015원"보다
알바생에게 설명하기 쉽다.

**리팩터링 지침**: 기준 시급 선정을 `private int resolveWeeklyRate(List<Work>)`
같은 **단일 메서드로 분리**할 것. Q9의 노무 확인 결과에 따라 기준이 다시 바뀌어도
그 한 곳만 고치면 되도록 한다.

### Q11 → C-6 수정 후 착수, 플래그 방식

새 엔드포인트를 만들지 않는다. `SalaryUpdateRequest`에 플래그를 추가한다:

```java
@Schema(description = "이후 근무에 새 시급을 일괄 적용할지 여부", defaultValue = "false")
private boolean applyToFutureWorks;   // 기본 false — 자동 소급이 아니다
```

기존 `PATCH /workplaces/{id}/workers/{workerId}`가 한 트랜잭션에서
① `salaries` 갱신 → ② 플래그가 켜져 있으면 `work_date >= 오늘`인 근무의
`hourly_rate` 일괄 UPDATE → ③ 영향받은 **모든 주** 범위 재계산.

**프론트 반복 호출을 배제한 이유**: 반복 근무 상한이 365일(`WorkService.java:68`)이라
주5일이면 260건 — ① 260회 왕복, ② 중간 실패 시 **한 주 안에 시급이 섞인 상태**가
남아 정정하려다 Q10의 문제를 만든다, ③ 매 PATCH마다 `recalculateWorkWeek`이 그 주
전체를 재계산해 같은 주를 5번 돈다, ④ 권한 검증 260회.

**C-6 선행 이유**: 위 ③의 "영향받은 모든 주 범위 재계산"이 C-6이 요구하는
`recalculateWorkWeeks(workerId, from, to, salary)`와 **동일한 기계**다. C-6을
고치면서 만들면 Q11은 그 위에 플래그 하나 얹는 것으로 끝난다.

---

## 잘 된 점

- **야간 시간대 판정식이 정확하다.** `SalaryCalculationService.java:156`의
  `isAfter(22:00) || equals(22:00) || isBefore(06:00)`가
  `[22:00,24:00) ∪ [00:00,06:00)`을 정확히 구현. 경계 검증: `22:00` 포함 ✓,
  `21:59` 제외 ✓, `05:59` 포함 ✓, `06:00` 제외 ✓. 자정 넘김을 `LocalTime`
  비교만으로 해결한 것이 깔끔하다. **스코프 2도 독립적으로 같은 평가.**
- **주 단위 재계산이 멱등하다.** `recalculateWorkWeek`(79–122)이 저장된
  `base_pay`/`gross_income`을 누적하지 않고 매번 원본
  (`start_time`/`end_time`/`rest_time_minutes`)에서 전량 재계산한다. 급여
  계산에서 가장 흔한 드리프트 버그가 구조적으로 차단돼 있다.
- **월별 집계 N+1 제거.** `getWorkerMonthlyWorkplaceSummaryList`(320–334),
  `getOwnerMonthlyWorkplaceSummaryList`(519–550)가 `findAllBy...ListIn` 계열로
  선조회 후 in-memory 조립. 근무지·근무자 수와 무관하게 쿼리 수가 상수.
- **장기요양보험료 구조가 맞다.** `:678-680`이 건강보험료를 먼저 구하고 거기에
  요율을 곱한다(소득에 직접 곱하지 않음). 실제 부과 구조와 일치.
- **급여일 D-day의 말일 처리.** `:442-472`가 `withDayOfMonth(31)`이 2월에 던지는
  `DateTimeException`을 잡아 말일로 대체. 이번 달/다음 달 양쪽 다 처리했다.
- **요율이 설정으로 외부화돼 있다.** `application.properties:79-85` + `@Value`(56–72).

---

## Critical

### C-1 — 주휴수당 산식이 법정 산식이 아님 · 최대 2.5배 과다 지급

`SalaryCalculationService.java:99-104`

```java
double avgDailyWorkHours = (weeklyWorkMinutes / 60.0) / weekWorks.size();
weeklyHolidayAllowance = (int) (avgDailyWorkHours * weekWorks.get(0).getHourlyRate());
```

**"주 평균 1일 근로시간 × 시급"** 으로 계산한다. 통용되는 법정 산식은
`(1주 소정근로시간 ÷ 40) × 8 × 시급`이고 40시간 초과분은 인정되지 않는다(8시간 상한).
두 식은 **주 5일 근무일 때만 우연히 일치**한다.

시급 10,030원 기준:

| 근무 패턴 | 코드 | 법정 산식 | 차이 |
|---|---|---|---|
| 주 2일 × 10h (20h) | **100,300원** | 40,120원 | **+60,180 과다** |
| 주 3일 × 12h (36h) | **120,360원** | 72,216원 | **+48,144 과다** |
| 주 5일 × 8h (40h) | 80,240원 | 80,240원 | 일치 |
| 주 6일 × 3h (18h) | **30,090원** | 36,108원 | **−6,018 과소** |
| 주 6일 × 10h (60h) | **100,300원** | 80,240원 (상한) | **+20,060 과다** |

주 2~3일 단시간 알바가 주 사용자층이면 거의 모든 주휴수당이 2배 이상 과다 계산된다.

**수정**:
```java
double weeklyHours = Math.min(weeklyWorkMinutes / 60.0, 40.0);
weeklyHolidayAllowance = (int) Math.round(weeklyHours / 40.0 * 8.0 * hourlyRate);
```
`40`, `8`, `15`를 전부 설정으로 외부화할 것(현재 `15 * 60`만 인라인 리터럴).

> ⚠️ **법정 산식 적용 여부는 제품 확인 필요** — Q9 참조. 다만 현재 산식이
> 근무일수에 따라 결과가 뒤집히는 것은 산식 선택과 무관한 결함이다.

### C-2 — 주휴수당 분자와 분모가 서로 다른 집합에서 계산됨

`SalaryCalculationService.java:93-113`

```java
long weeklyWorkMinutes = weekWorks.stream()
        .filter(w -> w.getEndTime() != null)          // ← 필터 O
        .mapToLong(...).sum();
double avgDailyWorkHours = (weeklyWorkMinutes / 60.0) / weekWorks.size();  // ← 필터 X
int dailyHolidayAllowance = weeklyHolidayAllowance / weekWorks.size();     // ← 필터 X
List<Work> updatedWorks = weekWorks.stream()
        .filter(w -> w.getEndTime() != null)          // ← 다시 필터 O
        .map(...)
```

시급 10,030원, 월~금 5건 09:00–18:00 휴게 60분 + 토요일 1건 `end_time = NULL`:
- `weeklyWorkMinutes` = 2,400분(5건), `weekWorks.size()` = **6**
- 배분 총액 **55,720원** / 토요일 건을 지우면 **80,240원**
- → **미완료 근무 1건이 존재한다는 이유만으로 그 주 주휴수당 24,520원 증발**

확정 정책 1번(예정 `end_time` 기준 배분)과도 어긋난다. `end_time`이 NULL이면
예정 종료시간 자체가 없으니 배분 대상이 아닌 게 맞는데, **분모에서는 세고 있다.**

**수정**: 필터된 리스트를 한 번만 만들어 세 곳에서 공유.
```java
List<Work> payableWorks = weekWorks.stream().filter(w -> w.getEndTime() != null).toList();
```

### C-3 — 급여 설정을 바꿔도 근무 급여가 재계산되지 않음

`WorkerService.java:197-222`, `:230-253`, `WorkplaceService.java:219-243`

세 곳 모두 `salaryRepository.update(newSalary)` 직후
`recalculateEstimatedNetIncomeForMonth`만 호출한다. `recalculateWorkWeek`은
**어디서도 호출하지 않는다.** 전자는 `works.gross_income`을 읽기만 하고
`estimated_net_income`만 갱신하므로, `hourly_rate`·`base_pay`·`night_allowance`·
`holiday_allowance`·`gross_income`은 손대지 않는다.

시급 10,030 → 11,000 인상, 다음 달 예정 근무 20건 각 8시간:
- 20건의 `hourly_rate`는 **10,030 그대로**, `base_pay`도 80,240 그대로
- 월 세전: 코드 **1,604,800원** / 정답 **1,760,000원** → **155,200원 미지급**
- `estimated_net_income`만 다시 써져서 겉보기엔 "재계산이 돌았다"고 착각하게 만든다

같은 이유로 `hasNightAllowance`를 켜도 기존 근무의 야간수당은 영원히 0,
`salaryCalculation`을 HOURLY→FIXED로 바꿔도 근무 행은 시급 기준 값을 유지한다.

**수정 방향은 Q1(시급 스냅샷 정책) 확정 후.** 다만 현재처럼 절반만 재계산해
데이터가 서로 모순되는 상태는 어느 정책으로도 틀렸다.

### C-4 — `night_work_minutes`(사실 기록)가 수당 지급 여부에 종속

`SalaryCalculationService.java:150-160` — `if (hasNightAllowance)` 안에서만
`nightWorkMinutes++`. `night_work_minutes`는 "몇 분이 22–06시였는가"라는 **사실**,
`night_allowance`는 "얼마를 더 주는가"라는 **정책**이다. 정책 플래그로 사실 기록을 껐다.

`:495`의 `.dayTimeMinutes(totalWorkMinutes - totalNightMinutes)`에 즉시 드러난다.
`has_night_allowance = false`인 알바생이 22:00–06:00 심야만 20건(각 480분):
- 코드: `nightTimeMinutes` **0분**, `dayTimeMinutes` **9,600분**
- 정답: `nightTimeMinutes` 9,600분, `dayTimeMinutes` 0분
- → 야간 전담 알바생 홈 화면이 "야간 근무 0시간"

**수정**: 야간 분 카운트는 무조건 수행, `hasNightAllowance`는 금액 계산(`:172`)에만 적용.

### C-5 — `night_work_minutes`가 휴게시간을 제외하지 않음 · `dayTimeMinutes` 음수

스키마 주석(`db/moup.sql:129`)은 `night_work_minutes INT -- 야간 근무시간(분, 휴게시간 제외)`인데,
코드는 gross 커서 루프에서 세고 휴게를 빼지 않는다. `netWorkMinutes`만 `:164`에서 뺀다.

시급 10,030, 22:00–06:00, 휴게 60분:
- `gross` 480, `net` **420**, `night` **480**
- `nightAllowance` = `(int)(480/60.0 × 10,030 × 0.5)` = **40,120원** /
  휴게 제외 시 **35,105원** → **5,015원 과다**
- `:495` → `dayTimeMinutes` = 420 − 480 = **−60분**. API가 음수 분을 내려보낸다.

**수정**: 야간 분에도 휴게를 비례 배분하거나, 최소한
`nightWorkMinutes = Math.min(nightWorkMinutes, netWorkMinutes)`로 클램프.
스키마 주석과 코드 중 하나를 진실로 확정할 것.

### C-6 — 재계산 범위가 변경 범위보다 좁음 (스코프 2 C4 확증 + 확장)

`recalculateWorkWeek(workerId, date, salary)`는 **날짜 1개가 속한 1주**만 재계산하고,
이어서 **그 날짜가 속한 1개 달**의 `estimated_net_income`만 갱신한다(`:121`).

**(a) 근무 날짜 이동 시 이전 주 스테일** — `WorkService.java:913-922`
시급 10,030, A주 월/화/수 각 8시간(24h). 수요일 건을 다음 주로 이동:
- A주 정답: 2건 16h → 일별 16,048원
- A주 DB 잔존: 일별 26,746원 → **21,396원 과다 지급이 남음**

**(b) 반복 근무 대량 삭제 시 대부분 주 스테일** — `WorkService.java:664-669`
3개월치(12주) 삭제 시 기준 주 1개만 재계산, 나머지 11주는 삭제 전 분모 기준 값 유지.
`deleteWorkHelper`, `stopRecurrenceAndUpdateSingle`, `replaceWithNewRecurringWorks` 동일.

**(c) 주가 월 경계를 넘을 때 이전 달 `estimated_net_income` 스테일** ← **스코프 3 신규**
2025-09-29(월)~10-05(일) 주에서 10-02 근무 수정 시,
`updateWorkWeekDetailsBatch`(`:117`)는 09-29·09-30의 `gross_income`까지 갱신하지만
`:121`은 `date.getMonthValue()` = **10월만** 재계산한다.

**수정**: 급여 도메인이 범위 기반 API를 노출해야 한다 —
`recalculateWorkWeeks(workerId, LocalDate from, LocalDate to, Salary)`가
`[from..to]`의 모든 월요일을 순회하고, 그 뒤 영향받은 **모든 달**에
`recalculateEstimatedNetIncomeForMonth`를 돌리는 구조. 호출부는
"변경 전 날짜"와 "변경 후 날짜"를 둘 다 넘겨야 한다.

---

## Important

### I-1 — 실제 퇴근 시각이 급여에 전혀 반영되지 않음 (스코프 2 M5 심화)

`WorkService.java:619-631` + `WorkRepository.java:281` + `SalaryCalculationService.java:140`

`WorkRepository:281`의 `end_time = COALESCE(end_time, #{actualEndTime})`가 기존 값을
유지하고, `calculateDailyIncome`은 `end_time`만 본다. 따라서 **예정 종료시간이 설정된
근무는 실제 몇 시에 퇴근하든 급여가 1원도 바뀌지 않는다.** `needsRecalculation`이
true여도 재계산 결과가 입력과 동일해 그 분기 전체가 no-op다.

시급 10,030, 09:00–18:00 예정(휴게 60분), 15:00 조퇴:
- 코드: `net` 480분 → `base_pay` **80,240원**
- 실제 근로(5h − 휴게): **50,150원** → **30,090원 과다 지급**, 조퇴가 완전히 무시됨

확정 정책 1번은 "**실제 퇴근 기록이 없는**" 경우만 정의한다. 있고 예정과 다른 경우는
미정의 → **Q2**.

### I-2 — `works.hourly_rate` NULL 레거시 행 하나로 그 주 전체가 500 (스코프 2 M11 확증)

`SalaryCalculationService.java:103`의 `weekWorks.get(0).getHourlyRate()` 언박싱 NPE.
`Work.hourlyRate`는 `Integer`(`Work.java:24`), 컬럼은 `hourly_rate INT NULL`(`db/moup.sql:132`).
현재 쓰기 경로는 항상 채우므로 신규 행은 안전하나, **운영 DB에 NULL 행이 있으면 그 주의
모든 근무 조회/수정/삭제가 500**으로 실패한다.

부수적으로 `weekWorks.get(0)`은 `ORDER BY work_date`의 **가장 이른 근무**를 임의로 고른다.
주 중간에 시급이 바뀌면 그 주 전체가 옛 시급으로 계산된다.

**수정**: null 방어 + 주휴수당 기준 시급을 `salary.getHourlyRate()`로.
배포 전 `UPDATE works SET hourly_rate = 0 WHERE hourly_rate IS NULL` 마이그레이션 필요.

### I-3 — `salaryUpdateRequest`/`salaryCreateRequest`에 `@NotNull` 누락 → NPE 500

`BaseWorkerUpdateRequest.java:18-20`, `WorkerWorkplaceUpdateRequest.java:22-24`,
`WorkerWorkplaceCreateRequest.java:23-25` — `@Valid`만 있고 `@NotNull`이 없다.
`@Valid`는 null을 통과시킨다. 필드 생략 시 `WorkerService.java:199`,
`WorkplaceService.java:221`, `:93`에서 **NPE 500**.

`WorkplaceJoinRequest.java:25-28`은 `@Valid @NotNull`을 제대로 붙였다 — 나머지 3곳이
그 컨벤션에서 빠진 것뿐이다.

### I-4 — `hourlyRate`/`fixedRate` 조건부 필수 검증 없음 → 전 급여 0원

`SalaryCreateRequest.java:20-28`, `SalaryUpdateRequest.java:20-28`.
`@Positive`는 null을 통과시킨다.
- `HOURLY` + `hourlyRate = null` → 통과 → `WorkService.java:758`에서 0 →
  **모든 급여가 0원으로 조용히 기록되고 에러가 나지 않는다**
- `FIXED` + `fixedRate = null` → `:229, 390, 598`에서 0 → 월급 0원

`salaryType`/`salaryDate`/`salaryDay`도 동일한 커플링(MONTHLY면 `salaryDate` 필수,
WEEKLY면 `salaryDay` 필수). 현재는 null이면 `daysUntilPayday`가 조용히 null(`:444, 475`).

**수정**: `@AssertTrue` 또는 클래스 레벨 제약.

### I-5 — 최저임금 검증이 어디에도 없음

`SalaryCreateRequest.java:23`, `SalaryUpdateRequest.java:23` — `@Positive`가 전부.
시급 1원도 통과한다. 서비스 레이어에도 하한 검증이 없다.
Swagger `example`의 `10030`(`:24`)은 문서용 예시일 뿐 검증이 아니다.
액수는 제품 결정 → **Q4**.

### I-6 — 소득세 3.3%에 지방소득세 10%를 다시 더해 이중 공제

`SalaryCalculationService.java:687-692` + `application.properties:83`
(`salary.rates.simple-income-tax=0.033`)

`0.033`은 사업소득 원천징수 **총액**(소득세 3% + 지방소득세 0.3%)이다.
여기에 다시 10%를 곱해 더하면 실효 **3.63%**.

세전 1,000,000원: 코드 **36,300원 공제** / 3.3% 기준 **33,000원** → **3,300원 과다 공제**

더 근본적으로 **근로소득(4대보험 + 간이세액표)과 사업소득(3.3%)을 구분하지 않는다.**
4대보험 플래그가 켜진 근로소득자에게도 `has_income_tax`가 켜지면 사업소득 요율이 적용된다 → **Q5**.

### I-7 — 고정급 근무자의 캘린더 일급이 0원으로 남음

`SalaryCalculationService.java:80-82`가 고정급이면 `recalculateWorkWeek` 전체를 조기 반환한다.
그런데 `calculateDailyIncome`은 `WorkService.java:763, 801, 908`에서 **고정급 여부와 무관하게
호출**되어 시급 기준 값을 쓴다. 고정급 근무자는 보통 `salaries.hourly_rate`가 NULL →
`hourlyRate = 0` → **`base_pay = 0`, `gross_income = 0`이 DB에 기록**된다.

월급 2,156,880원 고정급, 그 달 근무 22건:
- 각 근무 행: `base_pay` 0, `gross_income` 0
- 월 요약(`:399-401`): `grossIncome` **2,156,880원**
- 일별 캘린더: **0원**

`WorkService.java:289, 1027`의 `estimatedNetIncome(isFixedSalary ? null : ...)`가
최종 화면은 가리지만, DB에는 의미 없는 0이 저장되고 시간 컬럼도 주 단위로는
영원히 재계산되지 않는다.

**수정**: 고정급일 때 시간 컬럼은 계산하고 금액만 0/NULL로 두도록 분기.
조기 반환도 **금액 계산만** 건너뛰도록 좁힐 것(시간 집계는 `:363-373`이 쓴다).

### I-8 — 사장님은 급여를 쓸 수 있는데 읽을 수 없음 → 블라인드 덮어쓰기

`SalaryDetailResponse` 생성 지점은 `WorkplaceService.java:113-129` 하나뿐이고
`ROLE_WORKER` 케이스에만 존재한다. 사장님은 `OwnerWorkerUpdateRequest`로 알바생 급여를
**전체 덮어쓸 수 있는데**, 그 급여를 **조회할 API가 없다.** `SalaryUpdateRequest`의
`salaryType`/`salaryCalculation`/`has*` 7개가 전부 `@NotNull`이라 사장님 클라이언트는
현재 값을 모른 채 모든 필드를 채워 보내야 하고, 알바생이 설정한 시급·주휴수당 여부·
4대보험 여부가 **통째로 날아간다.**

확정 정책 2번은 사장님의 급여 파생 수치 **조회**가 의도라고 했다. 지금은 정반대다.

**수정**: 사장님용 조회를 열거나, `SalaryUpdateRequest`를 부분 업데이트(PATCH)로 전환.
후자가 더 안전하다.

### I-9 — `SalaryDetailResponse`에 `fixedRate`/`salaryDay` 미설정 → 라운드트립 데이터 손실

`WorkplaceService.java:117-129`가 `SalaryDetailResponse`를 조립하면서
정의된 `fixedRate`(`:21-22`)와 `salaryDay`(`:25-26`)를 **세팅하지 않는다.**

고정급 근무자의 근무지 상세에서 `fixedRate = null`. 클라이언트가 이 응답을 수정 폼에
채워 되보내면 `SalaryRepository.java:56`이 무조건 세팅하므로 **`fixed_rate`가 NULL로
덮어써진다 → 고정급 근무자의 월급이 사라진다.**

**수정**: 두 필드를 채운다. 한 줄씩이다.

### I-10 — 근무 시간 상한 없음 · 분 단위 루프 DoS (스코프 2 I7 확증)

`WorkService.java:985-987`이 순서만 검사하고 길이 상한이 없다.
`SalaryCalculationService.java:150-162`는 1분씩 `plusMinutes` 루프.
`2020-01-01` → `2030-01-01`이면 **약 526만 회**, 매 반복마다 객체 할당.
`recalculateWorkWeek`이 그 주의 모든 근무에 대해 돌리므로 배수 증폭된다.

**수정 (2단)**: ① `verifyStartEndTime`에 24시간 상한. ② 루프를 구간 교집합 산술로
대체 — 상수 시간이 되고 C-5의 휴게 안분도 같이 처리하기 쉬워진다.

### I-11 — 휴일수당(공휴일 근로 가산) 미구현

`works.holiday_allowance`(`db/moup.sql:135`)는 주석·실사용 모두 **주휴수당**이다
(`:183`이 `dailyHolidayAllowance`만 씀). 법정공휴일 판정 로직이 코드 전체에 없고
관련 테이블도 없다.

**이중 계상 위험은 없다**(쓰는 곳이 한 곳뿐). 다만 컬럼명과 DTO 필드명
(`totalHolidayAllowance`, `:500`)이 "주휴수당"인지 "휴일수당"인지 구분되지 않아
나중에 같은 컬럼에 섞어 쓸 확률이 높다 → **Q6**.

---

## Minor

| # | 내용 |
|---|---|
| M-1 | **부동소수점 절삭으로 1원씩 손실.** `:169`, `:172`의 `(int)(분/60.0 × 시급)`. 시급 1,000~30,000(10원 단위) × 1~1,440분 전수 탐색 결과 **90,310건** 불일치(리뷰어가 `javac`로 실행 검증). 예: 시급 12,000 × 246분 → 코드 **49,199원**, 정답 **49,200원**. 시급 15,000 × 245분 → 61,249 / 61,250. (9,860·10,030은 우연히 0건.) 절삭은 **항상 근로자에게 불리한 방향**. **수정**: `(int)((long) netWorkMinutes * hourlyRate / 60)` — 정수 산술로 오차 원천 제거. 야간은 `/ 120`. |
| M-2 | **주휴수당 일별 배분 나머지 버림.** `:108` 정수 나눗셈. 80,240원 / 3일 = 26,746 × 3 = 80,238 → 2원 손실. 주당 최대 (근무일수−1)원. **수정**: 나머지를 첫(또는 마지막) 근무일에 몰아주기. |
| M-3 | **월 공제액을 근무일에 균등 배분.** `:286` + `WorkRepository.java:300-305`. ① 절삭 누적(100,000/22 = 4,545 × 22 = 99,990). ② **근무 길이 무시** — 3시간 근무와 10시간 근무에 같은 공제액. `GREATEST(0,...)`가 음수는 막지만 일급 합이 월 세후 총액과 불일치해 두 화면이 다른 숫자를 보인다. |
| M-4 | **보험 적용 소득 기준 220만원 하드코딩.** `:298-302`. 시간 기준(`insuranceMinHours`)은 설정으로 뺐는데 소득 기준만 인라인이고, 주석 스스로 "정책에 따라 변경 가능"이라 쓰여 있다. 동류: `:172` 야간 가산율 `0.5`, `:689` 지방소득세율 `0.1`, `:99` `15 * 60`. |
| M-5 | **`hasIndustrialAccident`가 저장만 되고 미사용.** 산재보험은 전액 사업주 부담이라 근로자 공제에 없는 게 맞으나, `@NotNull`로 필수 입력받아 아무 데도 쓰지 않는 건 혼란스럽다. 필드 설명에 "표시 전용" 명시. |
| M-6 | **죽은 DTO.** `SalarySummaryResponse.java`(27줄), `SalaryCreateResponse.java`(13줄) — 전 코드베이스 참조 0건. 삭제. |
| M-7 | **null 안전 스타일 불일치.** `:674, 677, 682, 687`은 `Boolean.TRUE.equals(...)`, `:84, 85, 485-488, 500-501`은 직접 언박싱. 스키마상 `has_*`가 전부 NOT NULL(`db/moup.sql:163-169`)이라 실제 NPE는 없으나 하나로 통일할 것. |
| M-8 | **주간 합계에 음수 클램프 없음.** `:95`. `calculateDailyIncome`은 `:165`에서 클램프하는데 주간 합계는 안 한다. 60분 근무에 휴게 120분 → 주간 합계에 **−60분** 기여. 개별 `net_work_minutes` 합(2,400)과 주간 합계(2,340)가 **달라진다.** 15시간·40시간 임계 근처에서 주휴수당 발생 여부를 뒤집을 수 있다. `:259-262`의 `totalMinutesWorked`도 동일. |

---

## 아키텍처 의견

**705줄이 세 가지 다른 일을 한다.**

1. **계산 엔진** (`calculateDailyIncome` 125–187, `calculateDeductions` 666–704,
   `isInsuranceApplicable` 298–302, 주휴수당 산식 93–113) — 약 130줄.
   **리포지토리 의존이 전혀 없는 순수 함수들**이다. `salary/domain/SalaryCalculator`로
   분리하면 스프링 컨텍스트 없이 단위 테스트가 가능해진다. **테스트 0건인 지금 이게
   가장 큰 실익이며, 사실상 테스트 작성의 선행 조건이다.**
2. **홈 화면 조회/조립** (306–513, 517–663) — **약 360줄, 파일의 51%**.
   유일한 호출자는 `HomeService`(45, 52, 71, 80). 이 두 메서드 때문에 급여 도메인이
   `WorkplaceRepository`·`WorkerRepository`·`UserRepository`를 끌어들인다(리포지토리 5개).
   계산 엔진 자체는 `WorkRepository` + `SalaryRepository` 둘이면 된다.
   `HomeService`로 옮기면 의존성 5 → 2.
3. **급여일 D-day 계산** (437–481, 약 45줄) — 급여 *계산*과 무관.
   `Salary.daysUntilPayday(LocalDate today)` 도메인 메서드로.

**중복**: 고정급 월 소득 계산이 **세 번 복붙**돼 있다 — 230–252, 398–423, 604–628.
세 곳이 미묘하게 다르다(첫째는 `daysWorked != 0` 가드, 둘째는 `workList.isEmpty()` 가드,
셋째는 가드 없음). 고정급 정책 변경 시 한 곳만 고치고 놓칠 구조.
`calculateFixedGrossIncome(...)` 하나로 합치면 약 60줄이 사라진다.

**컨트롤러가 없는 것 자체는 옳다.** 급여는 근무지/근무자에 종속된 값이고 독립
라이프사이클이 없으므로 `workplaces`/`workers` 엔드포인트에 실려 나가는 게 맞다.
다만 그 결과 생긴 비대칭(I-8)은 고쳐야 한다.

---

## 확인 질문 (8건)

| # | 질문 | 블로킹 |
|---|---|---|
| ~~**Q1**~~ | **답변 완료 → 스냅샷 유지 (미래 예정 근무·과거 근무 모두).** 아래 "확정 정책 3" 절 참조 | 해소 |
| **Q2** | 예정 종료시간이 있는 근무에서 **예정보다 일찍/늦게 퇴근**했을 때 급여는 예정 기준인가 실제 기준인가? 확정 정책 1번은 "실제 기록이 **없는**" 경우만 정의함 | **I-1 수정 방향** |
| **Q3** | 주휴수당 **개근 요건**(결근 시 미지급)을 반영해야 하는가? 현재 스키마에 결근 표현 컬럼이 없다. 또한 15시간 판정을 **소정근로시간**이 아니라 **실근로시간**으로 하고 있는데 의도된 근사인가? | C-1 |
| **Q4** | 최저임금 하한을 서버에서 강제할 것인가? (a) 저장 거부 (b) 경고만. 값은 설정 파일인가 DB인가(매년 변경되므로 배포 없이 갱신 필요할 수 있음) | I-5 |
| **Q5** | 급여 대상자는 **근로소득자**(4대보험+간이세액표)인가 **사업소득자**(3.3%)인가, 둘 다인가? 둘 다면 `salaries`에 소득 유형 컬럼 필요 | I-6 |
| **Q6** | 법정공휴일 근로 가산(50%)이 로드맵에 있는가? 있다면 `works.holiday_allowance` 재사용인가 별도 컬럼인가 | I-11 |
| **Q7** | 급여 원 단위는 **절사**인가 **반올림**인가? 현재는 일관되게 절사(항상 근로자에게 불리)인데 명시적 정책인지 `(int)` 캐스팅의 부작용인지 불명확 | M-1, M-2 |
| **Q8** | 사장님 홈의 "인건비"(`HomeService.java:72-76`)가 알바생 **세후** 소득 합계다. 사장님 관점 인건비는 세전 총액 + 사업주 부담 4대보험이 맞지 않는가? 현재 값은 실제 부담보다 작다 | — |
| ~~**Q9**~~ | **답변 완료 → (a) 법정 산식 채택.** `(min(주 근로시간,40)/40) × 8 × 시급`. 단 산식의 정확한 형태와 개근 요건(Q3)은 노무 확인 필요 | 해소 |
| ~~**Q10**~~ | **답변 완료 → 그 주 마지막 근무의 시급** (가중평균에서 변경). 선행 C-2 필수, I-2 완화, 정렬 tie-break 신규 필요 | 해소 |
| ~~**Q11**~~ | **답변 완료 → C-6 수정 후 착수.** 새 엔드포인트가 아니라 `SalaryUpdateRequest`에 `applyToFutureWorks` 플래그(기본 false)를 추가하고, C-6이 만드는 범위 재계산 API 위에 얹는다 | **C-6 선행** |

---

## 테스트 우선순위

`SalaryCalculator`(순수 함수)를 먼저 분리하면 1~6은 스프링 컨텍스트 없이 즉시 작성 가능.

| # | 대상 | 입력 | 기대 | 현재 |
|---|---|---|---|---|
| 1 | 주휴수당 산식 (C-1) | 시급 10,030, 주 2일 × 10h, 휴게 0 | **40,120원** | 100,300원 ❌ |
| 1b | 파라미터화 | 3일×12h / 6일×3h / 6일×10h | 72,216 / 36,108 / 80,240 | ❌ |
| 2 | 분자·분모 집합 일치 (C-2) | 시급 10,030, 월~금 5건 09–18 휴게 60 + 토 1건 `end_time=NULL` | **80,240원** | 55,720원 ❌ |
| 3 | 시급 변경 반영 (C-3, Q1 후 확정) | 10,030으로 20건 생성 → 11,000으로 수정 | 월 세전 **1,760,000원** | 1,604,800원 ❌ |
| 4 | 야간 기록의 수당 독립 (C-4) | 22:00–06:00, 휴게 0, `hasNightAllowance=false` | `night=480`, `allowance=0` | `night=0` ❌ |
| 5 | `dayTimeMinutes` 음수 방지 (C-5) | 22:00–06:00, 휴게 60 | `night ≤ 420`, `dayTime ≥ 0` | `dayTime=−60` ❌ |
| 6 | **야간 경계 회귀 방지 (현재 통과 — 먼저 고정할 것)** | 21–23시 / 05–07시 / 22:00–22:01 / 06:00–06:01 / 20–08시 / 23–02시 | 60 / 60 / 1 / 0 / 480 / 180 | ✅ 통과 |
| 7 | **멱등성 회귀 방지 (현재 통과)** | 임의 주 5건 → `recalculateWorkWeek` 3연속 | 3회 모두 동일 | ✅ 통과 |
| 8 | 날짜 이동 시 이전 주 재계산 (C-6a) | 시급 10,030, A주 월/화/수 8h → 수요일 건 B주로 이동 | A주 잔여 2건 합 **32,096원** | 53,492원 ❌ |
| 9 | 정수 산술 정확도 (M-1) | 시급 12,000, 순근무 246분 | **49,200원** | 49,199원 ❌ |
| 10 | 필수 필드 누락 (I-3, I-4) | `salaryUpdateRequest` 생략 / `HOURLY`+`hourlyRate` 없음 | 400 / 400 | 500 / 201+전액 0원 ❌ |

**6번과 7번을 가장 먼저 작성할 것** — 현재 정확한 두 동작이고, C-4/C-5/C-1 수정 중
깨뜨리기 가장 쉬운 지점이다.

---

## 총평

야간 시간대 판정식, 주 단위 재계산 멱등성, 월별 집계 N+1 제거는 정확하게 구현돼 있어
골격은 신뢰할 만하다. 그러나 **금액을 직접 결정하는 경로 6곳이 조용히 틀린 값을 쓴다** —
주휴수당 산식이 주 5일 미만 근무자에게 최대 2.5배 과다 지급되고(C-1), 분자·분모가
서로 다른 집합에서 계산돼 미완료 근무 1건만 있어도 주휴수당이 30% 증발하며(C-2),
시급을 인상해도 예정 근무의 `base_pay`가 갱신되지 않고(C-3), 날짜 이동·반복 삭제 시
영향받은 주 대부분이 옛 값을 유지한다(C-6). 크래시가 없어 운영 중 발견되지 않고
누적되는 유형이며, **테스트가 0건이라 어느 것도 잡히지 않는다.**

병합 전 최소 조건은 C-1·C-2 수정과 테스트 1·2·6·7 추가. C-3와 C-6은 Q1의 답이
나와야 올바른 수정 방향이 정해지므로 제품 결정을 먼저 받고 별도 PR을 권한다.
아키텍처 정리 중 **순수 계산기 분리는 테스트 작성의 선행 조건**이라 C-1 수정과
같은 PR에서 함께 하는 것이 효율적이다.
