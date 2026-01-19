FROM eclipse-temurin:17-jre

WORKDIR /app

# 한국 시간대(Timezone) 설정
RUN apt-get update && apt-get install -y --no-install-recommends tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apt-get purge -y --auto-remove tzdata && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 jar 파일을 복사
COPY build/libs/*.jar app.jar

# 컨테이너 시작 시 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]