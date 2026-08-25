# 코드 리뷰 원장

전체 코드베이스를 7개 스코프로 나눠 순차 리뷰하고, findings를 여기에 적재한다.
**리뷰가 전부 끝난 뒤 파일 단위로 묶어서 한 번에 수정한다.**

## 왜 리뷰 도중에 고치지 않는가

1. **리뷰어가 움직이는 타깃을 보게 된다.** 스코프 3 리뷰어가 이미 수정된
   `SalaryCalculationService`를 읽으면 적재된 finding과 대조가 안 되고,
   고치다 만 흔적을 새 버그로 오인한다. 재검증 비용이 리뷰 비용보다 크다.
2. **부분 시야로 내린 진단이기 때문이다.** 스코프 2 리뷰어가 짚은 급여/루틴
   이슈는 **경계에서** 본 것이다. 해당 파일을 통째로 보는 스코프 3·6 리뷰어가
   다른 진단을 낼 수 있다.
3. **한 파일을 여러 번 건드리게 된다.** `SalaryCalculationService` 하나에
   스코프 2에서만 5건(C1·C4·I6·I8·M11)이 걸려 있다.

**예외**: 사람이 실제로 겪어 보고한 파손이면서 수정이 한 줄이고 리뷰 대상
로직을 바꾸지 않는 경우. 지금까지 C2 하나가 해당됐고, develop에 직접 커밋했다.

## 리뷰 스코프

| # | 스코프 | 범위 | LOC | 상태 |
|---|---|---|---|---|
| 1 | 인증·토큰·시큐리티 | `domain/auth/`, `global/security/`, `JwtUtil`, `AppleJwtUtil`, `SecurityConfig` | ~1,450 | 대기 |
| 2 | 근무(Work) | `domain/work/` | 2,600 | **완료** → [scope-2-work.md](scope-2-work.md) |
| 3 | 급여 계산 | `domain/salary/` | 1,059 | **완료** → [scope-3-salary.md](scope-3-salary.md) |
| 4 | 사용자·알바생 | `domain/user/` | ~2,590 | 대기 |
| 5 | 근무지·초대코드 | `domain/workplace/` | ~1,460 | **완료** → [scope-5-workplace.md](scope-5-workplace.md) |
| 6 | 루틴·알람·FCM | `domain/routine/`, `domain/alarm/`, `global/infra/fcm/` | ~1,970 | 대기 |
| 7 | 횡단 관심사·인프라 | `global/config/`, `global/error/`, `global/common/`, `global/infra/`, `global/util/`, 빌드·배포 설정, `db/moup.sql` | ~1,050+ | 대기 |

진행 순서: **1 → 2 → 3 → 5 → 4 → 6 → 7** (보안 먼저, 금전 계산 다음, 나머지는 위험도 순)

선택 사항으로 축을 바꾼 8번 패스: MyBatis Repository 12개 전수 (SQL 인젝션,
동적 SQL 빈 컬렉션, `SELECT *`, 인덱스, N+1을 한 번에 대조).

## 리뷰어에게 기존 findings를 주지 않는 이유

독립적으로 같은 것을 짚으면 그것이 확증이고, 반박하면 그것이 더 값진 정보다.

**스코프 5에서도 확증이 나왔다.** `docs/review/`를 차단한 상태에서 스코프 2의
I2(`is_accepted` 미검사)를 독립적으로 짚었고, 스코프 2가 "근무 도메인이 검사하지
않는다"까지였던 것을 **전체 코드베이스에 읽는 곳이 0건**임을 확인하고 스키마 결함까지
확장해 Important → **Critical**로 올렸다.

**스코프 3에서 이 방식이 실증됐다.** 리뷰어에게 `docs/review/`를 열지 말라고
지시했는데, 스코프 2의 findings 7건을 독립적으로 동일하게 짚었다(I8→C-2,
I6→C-4, C4→C-6, I7→I-10, M11→I-2, C1 일부→M-8, M5→I-1). 그중 셋은 심각도가
올라갔고, C-6은 월 경계 케이스가 추가됐다.

## 리뷰 도중 수정하지 않은 결정의 실증

스코프 3의 **C-1(주휴수당 산식 자체가 법정 산식이 아님)** 이 이 결정을 정당화했다.

스코프 2는 경계에서만 봤기에 분모 불일치(I8)까지만 발견했다. 그때 I8을 고쳤다면
**근본적으로 틀린 산식의 분모만 고친 상태**가 되어 주 5일 미만 근무자에게 최대
2.5배 과다 지급이 그대로 남았을 것이고, 테스트까지 그 잘못된 기대값으로 고정됐을
것이다. `SalaryCalculationService` 705줄을 통째로 본 리뷰어만 찾을 수 있는
결함이었다.

## 수정 단계 진입 조건

7개 스코프 리뷰가 모두 끝나면:

1. 전체 findings를 **파일 단위**로 재그룹핑
2. 스코프 간 진단이 충돌하는 항목을 먼저 판정 (넓은 시야를 가진 쪽이 우선)
3. 심각도 순으로 수정 + 각 Critical에 회귀 테스트 1개
4. 이 브랜치(`fix/code-review-findings`)에서 작업

## 확인 질문 — 답변 완료

| 출처 | 질문 | 답변 | 반영 |
|---|---|---|---|
| 스코프 2 · 미확인 | 운영 `DATABASE_URL`에 타임존 파라미터가 붙어 있는가? | **붙어 있으나 무해** — 아래 참조 | 종결 |
| 스코프 2 · I10 | 사장님이 알바생 근무를 **삭제**할 수 있어야 하는가? | **예, 삭제 가능해야 함** | 현재 동작이 정상. Spec 문서화만 필요 → Minor로 강등 |
| 스코프 2 · I8 | 퇴근 미기록 근무의 주휴수당 배분 방식은? | **실제 퇴근 기록이 없으면 근무 시간대(예정)로 배분** | 현재 분자 방식이 정책과 일치. 분모만 수정 |
| 스코프 3 · Q1 | 급여 설정 변경 시 기존 근무의 급여는? | **등록 시점 스냅샷 유지 (미래·과거 모두)** | C-3은 호출 제거로 확정. I-2 권고 정정, C-7 신규 발견 → [scope-3-salary.md](scope-3-salary.md#확정-정책-3--급여-스냅샷-q1-답변) |

### 타임존 판정 (종결)

**안전하다.** 세 값이 전부 일치한다:

| 위치 | 값 |
|---|---|
| `.github/workflows/deploy.yml:32` | `serverTimezone=Asia/Seoul` |
| `docker-compose.prod.yml:45` | mysql 컨테이너 `TZ: Asia/Seoul` |
| `docker-compose.prod.yml:49` | `--default-time-zone=Asia/Seoul` |

`docker-compose.dev.yml:43`, `:48`도 동일해 dev/prod 불일치가 없다.

근거 두 겹:

1. **출발지 = 도착지.** 드라이버가 변환을 적용하더라도 소스와 타깃 존이 같아
   no-op다. 이 논거는 아래 2번의 세부 동작과 무관하게 성립한다.
2. **엔티티가 timezone-free 타입이다.** `Work.java:14-18`이
   `LocalDate`/`LocalDateTime`이며, Connector/J(9.2.0, `build.gradle:49`)는
   이 타입들을 리터럴로 전송하고 `connectionTimeZone`/`serverTimezone`을
   적용하지 않는다.
   *(주의: 이 항목은 Connector/J 매뉴얼로 재확인하지 못했다 — Context7의
   dev.mysql.com 코퍼스에 서버 내부 doxygen만 색인돼 있다. 1번 논거만으로
   결론이 서므로 추가 확인은 하지 않았다.)*

**부수 검증**: 전체 코드베이스에 존 없는 `LocalDate.now()` /
`LocalDateTime.now()` / `LocalTime.now()`가 **0건**이다. `new Date()` 4건은
전부 `JwtUtil`/`AppleJwtUtil`의 `issuedAt`으로, 절대 시각이므로 올바른 사용이다.

**잔여 리스크 (스코프 7에 적재)**: dev·prod 모두 **server(앱) 컨테이너에는 `TZ`가
없다** — mysql에만 있다. 코드가 항상 `SEOUL_ZONE_ID`를 명시해서 지금은 무해하지만,
JVM 기본 존이 이미지 기본값(대개 UTC)이라 존 없는 `now()`가 하나만 들어와도
날짜가 어긋난다. `docker-compose.*.yml`의 server 서비스에 `TZ: Asia/Seoul`
한 줄을 추가하는 무료 방어다.
