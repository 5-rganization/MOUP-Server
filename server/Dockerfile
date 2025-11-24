
# 1. Builder Stage (빌드)
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# 프로젝트의 모든 파일을 컨테이너로 복사 (캐싱 레이어 제거)
COPY . .

# gradlew 실행 권한 부여 및 빌드 (테스트 제외)
RUN chmod +x ./gradlew && \
    ./gradlew bootJar -x test --no-daemon

# 2. Runtime Stage (실행)
FROM eclipse-temurin:17-jre

WORKDIR /app

# 한국 시간대(KST) 설정 (필수 설정은 유지)
RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# 빌드 스테이지에서 생성된 JAR 파일만 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]