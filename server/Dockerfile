# ==================================
# STAGE 1: Build Stage (빌드 스테이지)
# ==================================
# openjdk:17-jdk-slim (Debian slim 기반, apt-get 포함) 사용, ARM64 플랫폼 명시
FROM --platform=linux/arm64 openjdk:17-jdk-slim AS build_stage

# 작업 디렉터리 설정
WORKDIR /app

# [추가] 빌드에 필요한 유틸리티(findutils for xargs, git for versioning if needed) 설치
# 한 번만 실행되도록 RUN 명령어 통합 및 캐시 정리
RUN apt-get update && apt-get install -y --no-install-recommends findutils git && \
    rm -rf /var/lib/apt/lists/*

# (캐시 최적화 1) Gradle Wrapper 파일 먼저 복사
COPY gradlew ./
COPY gradle/ ./gradle/

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# (캐시 최적화 2) 의존성 정의 파일 복사
COPY build.gradle settings.gradle ./

# (캐시 최적화 3) 의존성 다운로드 (소스 변경 시 이 단계는 캐시 재사용)
# Gradle 빌드 캐시를 활성화하고 Docker 볼륨 마운트를 활용 (docker-compose.yml 에서 설정 필요)
# --build-cache 옵션 추가
RUN ./gradlew dependencies --no-daemon --build-cache

# 나머지 소스 코드 복사
COPY src/ ./src/

# Gradle 빌드 캐시를 사용하여 bootJar 빌드 (테스트 제외)
RUN ./gradlew bootJar --no-daemon -x test --build-cache

# =====================================
# STAGE 2: Runtime Stage (런타임 스테이지)
# =====================================
# 더 가벼운 JRE 이미지 사용 시도 (eclipse-temurin은 ARM64 alpine 지원)
FROM --platform=linux/arm64 eclipse-temurin:17-jre-alpine

# 작업 디렉터리 설정
WORKDIR /app

# 한국 시간대(Timezone) 설정
# Alpine Linux는 glibc-utils 대신 tzdata 패키지 사용
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build_stage /app/build/libs/*.jar app.jar

# .env 파일에서 주입될 환경 변수들 (선언만)
ENV DATABASE_URL=
ENV DATABASE_USERNAME=
ENV DATABASE_PASSWORD=
ENV DATABASE_NAME=
ENV JWT_SECRET_KEY=
ENV KEYSTORE_PASSWORD=
ENV S3_BUCKET_NAME=
ENV AWS_ACCESS_KEY=
ENV AWS_SECRET_KEY=
ENV GOOGLE_CLIENT_ID=
ENV GOOGLE_CLIENT_SECRET=
ENV GOOGLE_REDIRECT_URI=
ENV APPLE_CLIENT_ID=
ENV APPLE_TEAM_ID=
ENV APPLE_KEY_ID=
ENV APPLE_PRIVATE_KEY=
ENV APPLE_REDIRECT_URI=
ENV REDIS_HOST=
ENV REDIS_PORT=
ENV REDIS_PASSWORD=
ENV FIREBASE_ACCOUNT_KEY_PATH=
ENV SPRING_PROFILES_ACTIVE=

# 컨테이너 시작 시 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

