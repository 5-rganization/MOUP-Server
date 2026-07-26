# MOUP Server 에이전트 작업 지침

## 프로젝트 개요

MOUP Server는 iOS 앱과 통신하는 Spring Boot API 서버다. 주요 비즈니스
도메인은 사장님과 알바생의 사업장, 캘린더와 루틴, 근태와 근무 기록,
급여 계산, 인증, 알림이다.

- 실행 환경: Java 17, Spring Boot 3.5.x, Gradle
- 영속성: MySQL 8
- 데이터 접근: JPA 마이그레이션 과정으로 JPA와 MyBatis가 함께 사용되고 있다.
- 캐시 및 임시 데이터: Redis 7
- 외부 연동: Firebase Cloud Messaging(FCM), Apple/Google OAuth, AWS S3, JWT
- API 진입점: Nginx 리버스 프록시

## 실행 구조

Docker Compose 환경의 주요 연결 구조는 다음과 같다.

```text
iOS 앱
   |
   v
Nginx
   |
   v
Spring Boot server
   |             |
   v             v
MySQL 8       Redis 7

Spring Boot server ---> Firebase Cloud Messaging ---> iOS 푸시 알림
```

- `nginx` 또는 `moup-nginx`가 외부 요청을 받아 `server:8080`으로 전달한다.
- `server`는 MySQL의 health check가 통과하고 Redis가 시작된 뒤 실행된다.
- `mysql`은 영속적인 비즈니스 데이터를 저장한다.
- `redis`는 캐시 등 일시적인 데이터를 담당한다.
- FCM은 Docker Compose 컨테이너가 아닌 외부 서비스다.
- 개발 및 운영 구성은 `docker-compose.dev.yml`, `docker-compose.prod.yml`에서
  관리한다.

## 작업 원칙

- 수정 전에 관련 도메인, 기준 스키마, 호출부, 테스트를 함께 조사한다.
- 사용자가 동작 변경을 요청하지 않았다면 기존 비즈니스 동작을 보존한다.
- JPA 마이그레이션 시 로컬 DB나 오래된 SQL 파일 하나만 기준으로 판단하지
  않는다. 운영 및 최신 기준 스키마를 먼저 확인하고 엔티티 매핑과 비교한다.
- Hibernate의 `create` 또는 `create-drop` 성공만으로 스키마 호환성을 판단하지
  않는다.
- 트랜잭션 경계는 애플리케이션/서비스 계층에 둔다.
- FCM과 같은 외부 네트워크 호출 중 DB 트랜잭션을 불필요하게 오래 유지하지
  않는다.
- 동작이 변경되면 관련 테스트를 추가하거나 수정한다.
- 테스트는 개발자의 로컬 MySQL 데이터를 변경하지 않도록 격리된 설정을
  우선 사용한다.
- `.env`, 인증 정보, Firebase 키, OAuth secret, JWT secret은 민감정보로
  취급한다. 출력하거나 커밋하지 않는다.
- 파괴적인 DB 또는 파일 작업은 정확한 대상을 먼저 확인하고 사용자의 명시적
  승인을 받은 뒤 실행한다.

## Git 및 배포 원칙

- 작업을 시작할 때 현재 브랜치, 작업 트리, diff를 먼저 확인한다.
- 사용자의 관련 없는 변경을 보존하며 명시적 허가 없이 폐기하지 않는다.
- 커밋 전에는 대상 diff 전체를 검토하고 변경 범위에 맞는 테스트를 실행한다.
- 테스트 실패와 의도적으로 보류한 테스트를 사용자에게 알린다.
- 커밋 메시지는 기존 Conventional Commit 형식을 유지하되 설명은 한글로 쓴다.
  예: `fix: Alarm 읽음 처리 트랜잭션 보완`
- 수정, 구현, 테스트 또는 커밋 요청은 push 권한을 포함하지 않는다.
- **변경 및 검증 결과를 사용자가 확인한 뒤 push를 명시적으로 요청하거나
  승인한 경우에만 push한다.**
- push 전에는 변경 내용, 테스트 결과, 대상 원격 브랜치, 알려진 위험과 후속
  작업을 요약하고 사용자 승인을 기다린다.
- 사용자가 명시적으로 요청하지 않으면 PR/MR을 생성하거나 수정하지 않는다.
- 미완성 마이그레이션이나 후속 작업이 남은 변경은 Draft PR을 우선 사용한다.
- 일반적인 feature/fix 브랜치의 통합 대상은 `develop`이다.
- `main`은 사용자가 별도로 지시하지 않는 한 안정화된 배포 버전에만 사용한다.
