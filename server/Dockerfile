FROM eclipse-temurin:17-jre

WORKDIR /app

# 한국 시간대(KST) 설정 (필수 설정은 유지). curl은 compose healthcheck가 쓴다.
RUN apt-get update && apt-get install -y --no-install-recommends tzdata curl && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 jar 파일을 복사
COPY build/libs/*.jar app.jar

# 컨테이너 시작 시 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]