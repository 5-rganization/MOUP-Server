# syntax=docker/dockerfile:1.7

# ==== Build ====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle wrapper & 설정 먼저 복사 (의존성 캐시 최대화)
COPY gradlew ./
COPY gradle/ ./gradle/
RUN chmod +x ./gradlew
COPY settings.gradle build.gradle ./

# Gradle 캐시 마운트로 의존성 예열(반복 빌드 빨라짐)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

# 소스만 마지막에 복사 → 이전 단계 캐시 최대 재사용
COPY src ./src

# 애플리케이션 빌드 (테스트 스킵)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon -x test bootJar

# ==== Runtime ====
FROM eclipse-temurin:17-jre
WORKDIR /app

# OS 패키지 설치 없이 JVM 타임존으로 해결(빠름)
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/*.jar app.jar

# 필요 ENV는 compose/.env에서 주입 (여기선 선언 불필요)
ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","app.jar"]
