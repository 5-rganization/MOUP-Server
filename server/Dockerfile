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