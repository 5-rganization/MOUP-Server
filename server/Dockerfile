# ==================================
# STAGE 1: Build Stage (빌드 스테이지)
# ==================================
# openjdk:17-jdk-slim 사용 (빌드는 성공했었음)
FROM --platform=linux/arm64 openjdk:17-jdk-slim AS build_stage

WORKDIR /app

# 빌드 유틸리티 설치
RUN apt-get update && apt-get install -y --no-install-recommends findutils git && \
    rm -rf /var/lib/apt/lists/*

# Gradle Wrapper 복사 및 권한 설정
COPY gradlew ./
COPY gradle/ ./gradle/
RUN chmod +x ./gradlew

# 의존성 파일 복사
COPY build.gradle settings.gradle ./

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon --build-cache

# 소스 코드 복사
COPY src/ ./src/

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon -x test --build-cache


# =====================================
# STAGE 2: Runtime Stage (런타임 스테이지)
# =====================================
# [수정] eclipse-temurin의 표준 JRE 버전 사용 (ARM64 지원 확실)
FROM --platform=linux/arm64 eclipse-temurin:17-jre

WORKDIR /app

# 한국 시간대(Timezone) 설정
# eclipse-temurin 표준 이미지는 Debian/Ubuntu 기반이므로 tzdata 사용
RUN apt-get update && apt-get install -y --no-install-recommends tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apt-get purge -y --auto-remove tzdata && \
    rm -rf /var/lib/apt/lists/*

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