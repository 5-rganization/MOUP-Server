# MOUP-Server
> MOUP 프로젝트의 백엔드 레포지토리입니다.

## 패키지 구조
```
📦 com.moup
 ┣ 📂 domain                    # 비즈니스 도메인 (Domain-Driven Structure)
 ┃ ┣ 📂 work                    # [Standard Structure Example]
 ┃ ┃ ┣ 📂 api                   # Presentation Layer (Controller)
 ┃ ┃ ┣ 📂 application           # Business Layer (Service)
 ┃ ┃ ┣ 📂 dao                   # Data Access Layer (Mapper/Repository)
 ┃ ┃ ┣ 📂 domain                # Domain Layer (Entity & Core Logic)
 ┃ ┃ ┃ ┣ 📜 Work.java           # Entity
 ┃ ┃ ┃ ┗ 📂 type                # Domain Enums (Status, Type etc.)
 ┃ ┃ ┣ 📂 dto                   # Data Transfer Objects (Request/Response)
 ┃ ┃ ┗ 📂 exception             # Domain Specific Exceptions
 ┃ ┣ 📂 alarm
 ┃ ┣ 📂 auth
 ┃ ┣ 📂 routine
 ┃ ┣ 📂 salary
 ┃ ┣ 📂 user                    # (Admin, Owner, Worker 통합)
 ┃ ┗ 📂 workplace
 ┣ 📂 global                    # 전역 공통 모듈
 ┃ ┣ 📂 common                  # 공통 DTO(Response), BaseEntity
 ┃ ┣ 📂 config                  # Spring 설정 (Web, Swagger, Redis, MyBatis)
 ┃ ┣ 📂 error                   # 전역 예외 핸들링 (Handler, ErrorCode)
 ┃ ┣ 📂 infra                   # 외부 인프라 연동 (S3, FCM, Mail)
 ┃ ┣ 📂 security                # 보안 설정 (JWT, Filter, Token)
 ┃ ┗ 📂 util                    # 정적 유틸리티 (DateUtil, StringUtil)
 ┗ 📜 MoupServerApplication.java
```

## Git 전략
> Git Flow 방식을 채택하여 배포와 개발을 분리했습니다.
[아키텍처 구조]
- main 브랜치에 develop 브랜치의 stable한 버전이 들어옵니다.
- develop 브랜치에 feature 브랜치들을 머지하고 버그를 해결합니다.
- feature, hotfix, 브랜치에 각각 기능 개발, 버그 수정이 이루어집니다.

## 외부 링크
### ERDCloud
https://www.erdcloud.com/d/T9FbqKsnaQiGiQJKf

### Issue Board
https://github.com/orgs/5-rganization/projects/1
