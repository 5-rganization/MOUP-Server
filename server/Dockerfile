# ==================================
# STAGE 1: Build Stage (빌드 스테이지)
# ==================================
# [수정] openjdk 이미지를 사용하고, 스테이지 이름(AS build_stage)과 platform 플래그 추가
FROM --platform=linux/arm64 openjdk:17-jdk-slim AS build_stage

# 작업 디렉터리를 /app으로 설정
WORKDIR /app

# [추가] 빌드에 필요한 유틸리티(xargs 포함) 설치
# apt-get update 후에 설치하고, 캐시 정리(rm -rf)하여 이미지 크기 최적화
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

# (캐시 최적화) 먼저 의존성 관련 파일만 복사합니다.
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle/ ./gradle/

# gradlew 실행 권한 부여 (openjdk 이미지에는 chmod가 있으므로 추가)
RUN chmod +x ./gradlew

# (캐시 최적화) 의존성을 먼저 다운로드합니다.
# RUN ./gradlew dependencies --no-daemon # openjdk 이미지에는 gradlew가 없으므로 주석 처리 (아래 bootJar에서 처리)

# 나머지 소스 코드를 복사합니다.
COPY src/ ./src/

# Gradle wrapper를 사용해 테스트를 제외하고 bootJar를 빌드합니다.
# --no-daemon은 CI/CD 환경에서 Gradle 데몬을 띄우지 않아 리소스를 아낍니다.
# [수정] gradlew 실행 권한 부여 후 실행
RUN ./gradlew bootJar --no-daemon -x test


# =====================================
# STAGE 2: Runtime Stage (런타임 스테이지)
# =====================================
# [수정] openjdk 이미지를 사용하고 platform 플래그 추가
FROM --platform=linux/arm64 openjdk:17-jdk-slim

# 작업 디렉토리를 /app으로 설정
WORKDIR /app

# 한국 시간대(Timezone) 설정 (로그 시간 등)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# [수정] --from=builder -> --from=build_stage 로 변경
COPY --from=build_stage /app/build/libs/*.jar app.jar

# .env 파일에서 주입될 환경 변수 (예시)
# docker-compose.yml에서 env_file로 실제 값이 주입됩니다.
ENV DATABASE_URL=${DATABASE_URL}
ENV DATABASE_USERNAME=${DATABASE_USERNAME}
ENV DATABASE_PASSWORD=${DATABASE_PASSWORD}
ENV DATABASE_NAME=${DATABASE_NAME}
ENV JWT_SECRET_KEY=${JWT_SECRET_KEY}
ENV KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD}
ENV S3_BUCKET_NAME=${S3_BUCKET_NAME}
ENV AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
ENV AWS_SECRET_KEY=${AWS_SECRET_KEY}
ENV GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
ENV GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
ENV GOOGLE_REDIRECT_URI=${GOOGLE_REDIRECT_URI}
ENV APPLE_CLIENT_ID=${APPLE_CLIENT_ID}
ENV APPLE_TEAM_ID=${APPLE_TEAM_ID}
ENV APPLE_KEY_ID=${APPLE_KEY_ID}
ENV APPLE_PRIVATE_KEY=${APPLE_PRIVATE_KEY}
ENV APPLE_REDIRECT_URI=${APPLE_REDIRECT_URI}
ENV REDIS_HOST=${REDIS_HOST}
ENV REDIS_PORT=${REDIS_PORT}
ENV REDIS_PASSWORD=${REDIS_PASSWORD}
ENV FIREBASE_ACCOUNT_KEY_PATH=${FIREBASE_ACCOUNT_KEY_PATH}

# 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

