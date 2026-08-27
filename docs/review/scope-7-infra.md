# 스코프 7 — 횡단 관심사 · 인프라

- **범위**: `global/config`, `global/error`, `global/common`, `global/util`, `global/infra`,
  빌드·배포 설정, nginx, `db/moup.sql`
- **판정**: **수정 후** — Critical 3건(C1·C2·C3) 수정 완료. **C7은 미조치(운영)**
- **집계**: Critical 7 (3건 수정 완료, **C7은 즉시 조치 필요**) / Important 16 / Minor 20 / 미확인 6
- **리뷰 격리**: `docs/review/` 차단. 확정 정책 4건 + 이미 검증된 사항(타임존·기존 테스트 실패·
  인증/급여 로직)을 전제로 제공해 재조사를 막았다

---

## ⚠️ 리뷰어 결론 정정 — cron은 실제로 돈다

리뷰어는 **C5(배치 미동작)** 를 "탈퇴 유예기간 하드 삭제 배치가 전혀 돌지 않는다"로
결론냈다. 근거는 타당했다 — `deploy.yml:74-82` crontab 블록 전체 주석 처리,
`delete_old_users.sh` 파일 부재, 앱 내 `@Scheduled` 0건, 그리고 `delete_old_users.log`에
남은 실패 기록.

**그러나 프로덕션 라즈베리 파이에는 cron이 수동 설정되어 실제로 동작한다**(제품 소유자 확인).
개발 서버에서만 안 돈다. 리뷰어는 이 정보를 갖고 있지 않았다.

### 이 정정이 C3의 심각도를 바꾼다 🔴

리뷰어는 "지금은 배치가 안 돌아 C3가 발현되지 않는다. C5를 고치는 순간 활성화되므로
**C3를 C5보다 먼저 고쳐야 한다**"고 했다. 순서 지적은 정확하지만 전제가 틀렸다.

**cron이 이미 돌고 있으므로 C3는 이미 발현 중이다.** 사장님이 탈퇴하고 유예 3일이 지나면
cron이 돌 때마다 그 근무지 알바생 전원의 근무·급여 이력이 CASCADE로 삭제된다.

> **피해 규모 조사는 하지 않는다** (제품 소유자 결정). 수정만 진행한다 — C2 → C3 순서.

---

## 배포 파이프라인 실체 — 겉보기와 다르다

### 살아있는 파이프라인은 Jenkins다. GitHub Actions는 코드를 배포하지 못한다.

```yaml
# docker-compose.dev.yml — server 서비스 (원장 관리자 확인 완료)
  server:
    image: neoskycladdocker/moup     # ← build: 섹션이 없다
```
```yaml
# deploy.yml:67
docker compose -f docker-compose.dev.yml up -d --build
```

**`--build`가 no-op이다.** 빌드할 컨텍스트가 없고, `up -d`는 `pull`이 아니라 로컬에 있는
이미지를 그대로 재사용한다. **두 compose 파일 모두 `build:` 섹션이 없음을 확인했다.**

→ develop에 푸시해도 GitHub Actions는 서버 코드를 갱신하지 않는다. 하는 일은
(a) Pi 워킹트리 `reset --hard`, (b) `.env` 재작성, (c) 컨테이너 재시작, (d) 이미지 prune뿐이다.

실제 이미지는 `Jenkinsfile:71-77`의 `docker buildx build --push`가 만들고
`:102-103`이 배포한다. `Dockerfile:12`가 `COPY build/libs/*.jar`인 것도 이를 뒷받침한다 —
Gradle 빌드는 이미지 밖에서 일어난다.

**두 파이프라인이 같은 호스트·같은 디렉터리를 노린다.** `Jenkinsfile:7,92`의
`neoskyclad` / `/home/neoskyclad/MOUP-Server`가 `delete_old_users.log:1`의 경로와 일치한다.
develop 푸시 한 번에 두 파이프라인이 동시에 git 조작을 한다.

### dev / prod compose 주요 차이

| 항목 | dev | prod |
|---|---|---|
| nginx | 80/443, **TLS 직접 종료**, certbot | 80만, 외부 NPM 뒤 |
| 호스트 포트 | `80:80`, `443:443` | 없음 (`proxy-net`로만) |
| MySQL 볼륨 | bind `~/moup-data/mysql` | named `moup-db-data` |
| watchtower | 없음 | 전 서비스 `enable=false` |
| `build:` | **없음** | **없음** |
| 이미지 태그 | 없음 → `:latest` | 없음 → `:latest` |

### `.env` 변수 중 소비처가 없는 것

| 변수 | 판정 |
|---|---|
| `KEYSTORE_PASSWORD` | ❌ `server.ssl.enabled=false`. 리포지토리 전체 grep 0건. **죽은 시크릿** |
| `ADMIN_AUTH_TOKEN` | ❌ 앱 코드에 없음. 원래 소비자는 주석 처리된 cron. **실제로는 Pi의 cron 스크립트가 쓴다** |
| `DATABASE_USERNAME` | ⚠️ compose에 `MYSQL_USER`가 없어 MySQL이 만드는 계정은 root뿐 → **앱이 root로 접속 중일 가능성** |
| `FIREBASE_ACCOUNT_KEY_PATH` | ⚠️ `classpath:` → **키가 jar 안, 즉 Docker Hub 이미지 안에** (I15) |

---

## 잘 된 점

- **시크릿이 커밋되지 않았다.** `git ls-files` 전수 확인 — `.env`, Firebase 키 모두 추적 이력
  0건. `git log --all -- 'keys/*.json'`도 비어 있다. **이 부분은 제대로 했다.**
- **`db/moup.sql`과 `db/init/moup.sql`이 완전히 동일하다** (md5 일치).
- **더미 데이터가 자동 초기화에 섞이지 않는다.** `db/init/`에는 `moup.sql` 하나뿐이고
  `moup_dummy_data.sql`은 마운트 대상이 아니다.
- **`hasRole` 접두사 처리가 옳다.** DSL은 `"WORKER"`, `@PreAuthorize`는 `'ROLE_OWNER'` —
  SpEL이 중복 접두사를 붙이지 않으므로 양쪽 정상. 흔한 함정을 피했다.
- **`GlobalExceptionHandler`의 422/400 분기가 정확하다** (`:43-61`) — 값 오류 422,
  JSON 문법 오류 400.
- **금액 `INT`는 KRW에 적절하다.** 최소 단위 1원, 상한 21억원. `DECIMAL` 불필요.
- **`AsyncConfig:22`의 `CallerRunsPolicy`** — 조용히 버리는 대신 백프레셔. 옳은 선택.
- **타임존 체인 일관.** `Dockerfile:6-9`가 앱 컨테이너까지 커버한다.

---

## Critical

### C1 — `@PreAuthorize` 인가 실패가 403이 아니라 500 ✅ **수정 완료** (`1008aea`)

```java
// GlobalExceptionHandler:113 — 확인 완료. AccessDeniedException 핸들러 없음
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<?> handleException(RuntimeException e) {
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;   // 500
```

`AuthorizationDeniedException extends AccessDeniedException extends RuntimeException`.
이 예외는 컨트롤러 프록시 안에서 발생하므로 `DispatcherServlet`이 `@RestControllerAdvice`로
먼저 넘긴다. catch-all이 매칭되어 정상 종료하므로 **`SecurityConfig:52-54`의
`accessDeniedHandler`는 예외를 보지도 못한다.**

**시나리오**: 알바생이 `POST /workplaces/1/works/batch`(`@PreAuthorize("hasRole('ROLE_OWNER')")`)
호출 → 403이어야 하는데 `COMMON_500` + HTTP 500. 각 컨트롤러의
`@ApiResponse(responseCode="403")` 문서와 전부 어긋나고, 정상적인 인가 거부가
서버 에러 알림을 계속 울린다.

**수정**: `@ExceptionHandler(AccessDeniedException.class)` 추가. catch-all보다 구체적이므로
우선 매칭된다. **투입 대비 효과가 가장 좋은 수정.**

### C2 — `PermissionVerifyUtil` NULL 역참조 (3번째 독립 확증) ✅ **수정 완료** (`f3feff7`)

`PermissionVerifyUtil:10,17`. 스키마에서 `owner_id BIGINT NULL`(`:95`),
`user_id BIGINT NULL`(`:108`) + `ON DELETE SET NULL`(`:114`) 확인.
호출자 17곳 어디에도 사전 null 가드가 없다.

스코프 2 I3, 스코프 4 #12에 이어 **세 번째 독립 확증.**

**수정**: `Objects.equals(...)` — null을 "불일치"로 취급(fail-closed).
같은 결함을 `WorkplaceService:253`, `WorkService:355`에서도 발견해 함께 고쳤다.

### C3 — `workplaces.owner_id ON DELETE CASCADE` ✅ **수정 완료** (`98ac8e9`) — ⚠️ 운영 DB 마이그레이션 필요

```sql
-- db/moup.sql:102 (확인 완료)
FOREIGN KEY (`owner_id`) REFERENCES users (`id`) ON DELETE CASCADE
```

연쇄: `users` → `workplaces` → `workers` → `works` + `salaries` + `work_routine_mappings`.

바로 옆 `workers.user_id`가 `SET NULL`인 것과 대비하면 비일관성이 명확하다.
**근무자는 보호하고 사장님은 보호하지 않는다.** `owner_id`가 이미 NULL 허용이므로
스키마 변경 없이 FK 동작만 바꾸면 된다.

**확정 정책 3과 정면 충돌.** 스코프 4 C4, 스코프 5 I-8과 동일 결함의 세 번째 확증.

**수정 순서 주의**: C2 수정이 선행되어야 `owner_id`가 NULL인 상태에서 403이 정상 반환된다.
→ C2를 먼저 커밋(`f3feff7`)한 뒤 C3(`98ac8e9`)를 커밋했다.

**스키마 파일 수정만으로는 운영 DB가 바뀌지 않는다.** 마이그레이션 도구가 없어
`db/migrations/2026-08-27-workplaces-owner-set-null.sql`을 수동 적용해야 한다
([배포 체크리스트 #4](applied-fixes.md#배포-체크리스트-)).

### C4 — `manual-db-init.yml`이 확인 절차 없이 프로덕션 DB를 날린다

`db/moup.sql:1-3`이 `DROP SCHEMA IF EXISTS moup; CREATE SCHEMA moup;`로 시작하고,
`manual-db-init.yml:10`의 `insert_dummy_data` 기본값이 **`true`** 다.
`workflow_dispatch`라 write 권한자면 누구나 버튼 하나로 실행할 수 있으며
확인 절차·환경 선택·백업이 **하나도 없다.**

**지금은 우연히 깨져 있다** — `:37`, `:46`의 `docker compose exec`에 `-f` 옵션이 없고
루트에 `docker-compose.yml`이 없어 설정 파일을 못 찾는다.
**"고쳤더니 DB가 날아가는" 최악의 형태다.**

**수정**: (a) `environment:` 승인 게이트 + 확인 문자열 입력, (b) 더미 데이터 기본값 `false`,
(c) 스키마 파일에서 `DROP SCHEMA` 제거 → `CREATE TABLE IF NOT EXISTS`, (d) 백업 스텝.

### C5 — 배치 실행 경로가 버전 관리 밖에 있다 (리뷰어 결론 정정본)

원 지적("배치가 전혀 안 돈다")은 프로덕션에 대해 틀렸다. 그러나 **드러난 사실들은 여전히 유효하다:**

- `deploy.yml:74-82` crontab 설정 전체 주석 처리 → **배포가 cron을 관리하지 않는다**
- `delete_old_users.sh`가 **git에 없다** (과거 커밋 `a07630c`에는 있었다).
  라즈베리에만 존재해 무슨 일을 하는지 검증 불가, 서버 교체 시 재현 불가
- `delete_old_users.log:1`의 경로에 **`server/` 세그먼트가 누락**돼 있다
- 앱 내 `@Scheduled` 0건

**소셜 연동 해제라는 중요한 파이프라인이 버전 관리 밖에 있다.**

**수정 방향**: 셸 스크립트 + crontab 대신 **앱 안으로 옮기는 것**이 낫다.
`@EnableScheduling` + `@Scheduled(cron="0 0 3 * * *", zone="Asia/Seoul")`.
그러면 `ADMIN_AUTH_TOKEN`(죽은 시크릿), HTTP 왕복, 별도 스크립트, cron 설정이 전부 사라진다.

⚠️ **단, cron과 `@Scheduled`를 동시에 두면 이중 실행된다.** 전환 시 Pi의 crontab을
반드시 제거할 것.

### C7 — Firebase 서비스 계정 키가 **public** Docker 이미지 안에 있다 🔴🔴 (I15에서 격상)

**Q1 확인 완료: `neoskycladdocker/moup`는 public이다.** (`is_private: False`, pull 1,461회,
최종 갱신 2026-08-25. 대조군으로 존재하지 않는 이름·계정은 HTTP 404 + 필드 부재를 확인해
검사 자체의 유효성도 검증했다.)

경로: `Jenkinsfile:26-32`가 빌드 **전에** 키를 소스 트리에 복사
→ Gradle이 jar에 패키징(`FIREBASE_ACCOUNT_KEY_PATH=classpath:keys/...`)
→ `Dockerfile:12`가 jar를 이미지에 복사 → `Jenkinsfile:76`이 `--push`로 public 업로드.

```bash
# 누구나 가능
docker pull neoskycladdocker/moup:latest
docker run --rm --entrypoint sh neoskycladdocker/moup -c \
  'cd /tmp && unzip -o /app/app.jar "BOOT-INF/classes/keys/*" && cat BOOT-INF/classes/keys/*.json'
```

Firebase 서비스 계정 키는 해당 프로젝트의 **FCM 전송 · Firestore · Auth 관리 권한**을 가진다.

**조치 순서 (① 최우선):**
1. **키 폐기 및 재발급.** 이미 노출됐으므로 리포지토리를 private으로 바꿔도 무의미하다 —
   이미 pull한 주체가 있다면 키는 이미 외부에 있다
2. 새 키는 **런타임 마운트**로. 코드 변경 0줄이다 —
   `volumes: [~/moup-secrets/firebase-key.json:/app/keys/firebase.json:ro]` +
   `FIREBASE_ACCOUNT_KEY_PATH=file:/app/keys/firebase.json`.
   `FCMConfig:27`의 `resourceLoader.getResource(...)`가 `file:` 스킴을 그대로 처리한다.
   그리고 `Jenkinsfile:26-32`를 삭제한다
3. Docker Hub 리포지토리를 **private으로**
4. Firebase 프로젝트 **감사 로그 확인** — 그 키로 비정상 접근이 있었는지

**다른 시크릿은 새지 않았다 (확인).** `.env`는 `Jenkinsfile:23-25`가 복사하지만 `Dockerfile`이
jar만 복사하므로 이미지에 포함되지 않는다(M10의 죽은 스테이지 덕). `application.properties`는
jar에 들어가나 값이 전부 `${...}` 플레이스홀더다. **노출된 것은 Firebase 키 하나다.**

### C6 — 프로덕션이 develop 이미지를 가져간다

`Jenkinsfile:66`이 `main` → `stable`, 그 외 → `latest`로 태깅한다.
그런데 `docker-compose.prod.yml:21`이 태그 없는 `image: neoskycladdocker/moup` →
**`:latest`, 즉 가장 최근 develop 빌드**를 받는다.

또한 `Jenkinsfile:99,128`이 `export TAG=...`를 넘기지만 **어느 compose도 `${TAG}`를
참조하지 않는다.** 태그 전략 전체가 죽어 있다.

**수정**: `image: neoskycladdocker/moup:${TAG:-stable}`. 롤백도
`TAG=release-42 docker compose up -d server`로 가능해진다.

---

## Important (요약)

| # | 내용 |
|---|---|
| **I1** | **Swagger가 프로덕션 무인증 공개.** `SecurityConfig:48`에 `// TODO: 나중에 swagger 비활성화 하기`. 프로파일 가드 없음. **수정은 프로퍼티 두 줄** (`springdoc.api-docs.enabled=false`) |
| **I2** | **프로파일 분리가 없다.** `application.properties` 하나뿐이고 유일한 분기는 `.env`. 프로덕션에 `web=DEBUG` + **Docker 로그 로테이션 없음**(기본 `max-size` 무제한) → 라즈베리 SD 카드가 찰 때까지 자란다 |
| **I3** | **`FCMService`가 `@Transactional` 안에서 FCM 네트워크 호출.** HikariCP 기본 10 커넥션이 FCM 지연에 인질 → **FCM 장애 = 서버 전체 정지.** 게다가 롤백 때문에 `:49` 주석("토큰 유무와 상관없이 히스토리 먼저 저장")의 의도가 실패 시에만 깨진다 |
| **I4** | FCM 전송 실패가 죽은 토큰을 정리하지 않음 (`UNREGISTERED` 미처리). `fcm_token`이 유저당 1개라 **다중 기기 불가** — 나중에 로그인한 기기만 푸시를 받는다 |
| **I5** | **`ADMIN_ALARM` 토픽에 구독자가 0명.** `subscribeToTopic` 호출이 코드베이스에 **0건**. 전체 공지 푸시가 아무에게도 안 가는데 FCM은 성공을 반환한다. 인앱 목록에는 뜨므로 "푸시는 안 오는데 앱을 열면 공지가 있다" |
| **I6** | checked 예외(`FirebaseMessagingException`, `FileUploadException`)가 핸들러를 통과해 Boot 기본 `/error` 응답으로 나간다 → 클라이언트 에러 파싱이 깨짐. `MaxUploadSizeExceededException`도 413이 아니라 500 |
| **I7** | **파일 업로드가 클라이언트 `Content-Type`만 믿는다.** 매직 바이트·크기·디코딩 검증 없음. `S3Service:39-44`가 원본 확장자를 그대로 이어붙여 **S3가 무료 파일 호스팅이 된다** |
| **I8** | 프로필 이미지 교체 시 **새 파일 업로드 전에 기존 파일을 삭제**. 업로드 실패 시 S3에 파일이 없고 DB는 죽은 URL을 가리킨다 |
| **I9** | **`normal_alarms`에 FK도 인덱스도 전무.** 모든 알림 조회가 풀스캔이고, 유저 삭제 시 고아 행이 영구 잔존 |
| **I10** | `works(worker_id, work_date)` 복합 인덱스 없음. FK가 만드는 단일 인덱스로는 filesort 발생. **스코프 2 M7 확증** |
| **I11** | `workers(workplace_id, user_id)` UNIQUE 없음 → check-then-insert 경쟁. **스코프 5 C-2 확증** |
| **I12** | CORS가 `SecurityConfig`와 `WebConfig` 두 곳에 다르게 정의. **Security 쪽이 이기는데 거기에 PATCH가 빠져 있다** → 웹 클라이언트를 붙이는 순간 PATCH 엔드포인트 전부 죽음 |
| **I13** | `/health`가 `return "OK"`뿐 — DB·Redis 미확인. compose의 server에 `healthcheck:` 블록도 없다. `spring-boot-starter-actuator`가 이미 있으므로 `/actuator/health`로 대체 |
| **I14** | `salaries`에 조합 제약 없음 — `HOURLY`인데 `hourly_rate IS NULL`인 행이 표현 가능 |

| **I16** | `ErrorCode.INVALID_TOKEN`이 401이 아니라 **400** → 클라이언트의 "401이면 갱신 후 재시도" 인터셉터가 동작하지 않는다 |
| **I17** | **레이트 리밋이 코드에도 nginx에도 없다.** `/auth/**`가 permitAll이라 무인증으로 소셜 검증 아웃바운드 호출을 무제한 유발 가능. **스코프 5 C-3 확증** |

---

## Minor (요약)

`delete_old_users.log` 추적 중 · `NameVerifyUtil.verifyName` 죽은 코드(게다가
`verifyNickname`과 규칙이 달라 `"12345678"`이 유효 닉네임) · `FCMService`의
`System.out.println` · `S3Service` URL에 리전 누락 + `extractKeyFromUrl`이 `indexOf(-1)`로
조용히 엉뚱한 키 생성 · **`spring-boot-starter-data-jpa`가 불필요**(JPA 사용 0건, MyBatis만
씀 — Hibernate가 기동 시 무의미하게 초기화) · `spring-cloud-starter-aws:2.2.6`(2020년,
AWS SDK v1) · `Jenkinsfile:23-25`의 `cp .env`가 죽은 스테이지(역설적으로 다행 — 이미지에
들어갔다면 시크릿이 Docker Hub로) · `SecurityConfig`의 `USER_AUTH_URL`에 `/works/**`,
`/home/**` 누락 · `TIMESTAMP`/`DATETIME` 혼용 · `AsyncConfig`에 종료 대기 설정 없어
**재시작 시 큐의 탈퇴 처리가 유실** · `deploy.yml:72`의 `prune -af`가 롤백 대상 삭제 ·
`nginx.prod.conf:28`의 `client_max_body_size 0` · `GlobalExceptionHandler:107`이 예외
메시지로 **내부 메서드·파라미터명 노출** · `:29`가 모든 4xx에 스택트레이스 ·
**`works`에 `created_at`/`updated_at`이 없어 급여 스냅샷 시점 감사 불가**

---

## 스키마 평가 — 마이그레이션 절차가 존재하지 않는다 🔴

### 두 파일의 관계

`diff db/moup.sql db/init/moup.sql` → **동일** (md5 일치). 지금은 동기화돼 있다.
그러나 손으로 유지해야 하는 두 사본이다.

| 적용 경로 | 파일 | 발동 조건 |
|---|---|---|
| 자동 초기화 | `db/init/moup.sql` | MySQL 데이터 디렉터리가 **비어 있을 때만** |
| 수동 초기화 | `db/moup.sql` | `workflow_dispatch` (현재 C4의 이유로 실행 실패) |

### 이미 배포된 DB에 컬럼을 추가할 정상 경로가 없다

Flyway도 Liquibase도 없다.
- 자동 초기화는 **운영 중 DB에 절대 적용되지 않는다**
- `manual-db-init.yml`은 마이그레이션이 아니라 **전면 파괴 후 재생성**이다

**현재 유일한 방법은 Pi에 SSH로 들어가 손으로 `ALTER TABLE`을 치는 것이다.**

> ⚠️ **이것이 확정 정책 5(가명처리)와 `workers.withdrawn_at`(스코프 4 권고) 작업의
> 실행 가능성을 직접 막는다.** 그 작업들은 전부 스키마 변경을 요구한다.

**권장**: Flyway 도입 (`V1__baseline.sql` + `baseline-on-migrate=true`). 부담이면 최소한
`db/migrations/`에 번호 붙인 `ALTER` 파일을 쌓고 적용 기록을 남기는 규율이라도 세울 것.
지금은 **적용 여부를 아무도 모른다.**

### 인덱스

이미 잘 골라진 것: `works.idx_repeat_group_id`, `routine_tasks.unique_routine_order` —
실제 쿼리와 잘 맞는다.

부족한 것: `normal_alarms.receiver_id`(FK 자체가 전무) · `works(worker_id, work_date)` ·
`workers(workplace_id, user_id)` UNIQUE · `workplaces(owner_id, workplace_name)` UNIQUE ·
`routines(user_id, routine_name)` UNIQUE · `user_tokens.user_id` UNIQUE

### `ON DELETE` 일관성

`workers.user_id`가 `SET NULL`인데 `workplaces.owner_id`가 `CASCADE`인 것이
**이 스키마의 핵심 비일관성**이다. `normal_alarms`는 FK 자체가 없다.

---

## 미확인 — 확인 필요

| # | 질문 | 확인 방법 |
|---|---|---|
| ~~**Q1**~~ | **확인 완료 → public이다.** `is_private: False`, pull 1,461회. 대조군(존재하지 않는 이름/계정)은 HTTP 404로 필드 자체가 없음을 확인해 검사의 유효성도 검증했다. **→ C7로 격상** | 해소 |
| **Q2** | `secrets.REDIS_HOST`가 `redis`인가? (로컬 `.env`는 `localhost`) | Pi에서 `grep REDIS_HOST ~/MOUP-Server/.env` |
| **Q3** | `secrets.DATABASE_USERNAME`이 `root`인가? root면 **앱이 DB root 권한으로 동작 중** | 위와 동일 |
| **Q4** | `secrets.DATABASE_NAME`이 `moup`인가? (`db/moup.sql:2`가 하드코딩) | 위와 동일 |
| **Q5** | Pi에 추적되지 않는 `docker-compose.yml`이 있는가? 있다면 **제3의 설정으로 운영 중**이고 이 리뷰의 compose 결론을 재검토해야 한다 | `ls -la ~/MOUP-Server/docker-compose*` |
| **Q6** | Jenkins가 실제로 활성인가? 꺼져 있다면 **아무것도 서버 코드를 배포하지 않고 있다**는 더 심각한 결론 | Jenkins 대시보드 + Pi에서 `docker inspect moup-server-dev --format '{{.Image}} {{.Created}}'` |
| **Q7** | `workers.user_id`가 NULL인 행이 존재하는가? C2의 심각도를 좌우 | `SELECT COUNT(*) FROM workers WHERE user_id IS NULL;` |

---

## 총평

가장 값진 발견은 개별 설정 오류가 아니라 **배포 파이프라인이 겉보기와 다르게 동작한다는
사실**이다. `build:` 한 줄의 부재 때문에 `--build`가 아무것도 하지 않고, GitHub Actions는
코드를 배포하지 못한다. 이걸 모른 채 "왜 푸시했는데 반영이 안 되지"를 디버깅하면 며칠이 사라진다.

**제대로 한 부분도 분명하다.** 시크릿 미커밋, 두 스키마 파일 동기화, 더미 데이터 격리,
타임존 일관성, `hasRole` 함정 회피, 400/422 시맨틱 — 인프라 리뷰에서 이 정도 기본기는 흔하지 않다.

문제는 **개발 초기의 편의 장치가 제거되지 않은 채 프로덕션으로 넘어왔다**는 점에 몰려 있다.
`DROP SCHEMA`로 시작하는 초기화 SQL과 더미 데이터 기본값 `true`, permitAll인 Swagger에 붙은
"나중에 비활성화하기" TODO, 프로파일 분리 없는 DEBUG 로그, 주석 처리된 채 잊힌 crontab과
그 실패를 증언하며 커밋되어 있는 로그 파일. 다섯 가지 모두 "일단 돌아가게 해놓고 나중에"의
흔적이고, 지금이 그 나중이다.

**권장 수정 순서**: **C7(키 폐기 — 코드가 아니라 지금 당장의 운영 조치)** → ~~C1~~(완료) → **C2 → C3 → C5** → C4 → C6 → I1 → I3 → I2.
앞의 넷은 서로 의존한다(C2는 C3 수정 후 `owner_id` NULL을 올바르게 다루기 위해 필요하고,
C3는 C5보다 반드시 먼저다). I1은 프로퍼티 두 줄이라 언제든 즉시 가능하다.
