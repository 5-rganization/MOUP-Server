# ==================================
# STAGE 1: Build Stage (빌드 스테이지)
# ==================================
# Gradle과 JDK 17이 포함된 이미지를 기반으로 "builder" 스테이지를 정의합니다.
FROM gradle:8-jdk17-alpine AS builder

# 작업 디렉터리를 /app으로 설정
WORKDIR /app

# (캐시 최적화) 먼저 의존성 관련 파일만 복사합니다.
# 이렇게 하면 소스 코드만 변경됐을 때 Docker가 이 레이어를 캐시합니다.
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle/ ./gradle/

# (캐시 최적화) 의존성을 먼저 다운로드합니다.
RUN ./gradlew dependencies --no-daemon

# 나머지 소스 코드를 복사합니다.
COPY src/ ./src/

# Gradle wrapper를 사용해 테스트를 제외하고 bootJar를 빌드합니다.
# --no-daemon은 CI/CD 환경에서 Gradle 데몬을 띄우지 않아 리소스를 아낍니다.
RUN ./gradlew bootJar --no-daemon -x test


# =====================================
# STAGE 2: Runtime Stage (런타임 스테이지)
# =====================================
# Java 17 실행 환경(JRE)만 포함된 가벼운 Alpine 이미지를 사용합니다.
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리를 /app으로 설정
WORKDIR /app

# 한국 시간대(Timezone) 설정 (로그 시간 등)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# "builder" 스테이지에서 빌드된 .jar 파일을 app.jar라는 이름으로 복사합니다.
# build/libs/에 .jar 파일이 하나만 있다고 가정합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# .env 파일에서 주입될 환경 변수 (예시)
# docker-compose.yml에서 env_file로 실제 값이 주입됩니다.
ENV JWT_SECRET=
ENV DATABASE_URL=
ENV DATABASE_USERNAME=
ENV DATABASE_PASSWORD=
ENV REDIS_PASSWORD=

# 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
