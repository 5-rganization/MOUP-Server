pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "neoskycladdocker/moup"
        TEST_SERVER_IP = "test.moup-server.com"
        SSH_USER = "neoskyclad"
        TARGET_BRANCH = "develop" 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Env') {
            steps {
                withCredentials([file(credentialsId: 'moup-env-file', variable: 'ENV_FILE')]) {
                    sh 'cp $ENV_FILE ./server/.env'
                }
            }
        }

        stage('Build Gradle') {
            steps {
                dir('server') {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build -x test'   // TODO: 추후에 테스트 포함하기 -> CI 구현
                }
            }
        }

        stage('Build & Push Docker') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-auth', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        dir('server') {
                            // 로그인
                            sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
                            
                            // [중요] Buildx를 사용하여 멀티 플랫폼 빌드 (혹은 arm64 강제 지정)
                            // 홈서버에 buildx가 설치되어 있어야 함 (최신 도커는 기본 내장)
                            def imageTag = "${TARGET_BRANCH}-${env.BUILD_NUMBER}"
                            
                            // --platform linux/arm64 옵션 추가!
                            // --push 옵션을 쓰면 build와 push를 한 번에 함
                            sh """
                                docker buildx create --use || true
                                docker buildx build --platform linux/arm64,linux/amd64 \
                                -t ${DOCKER_IMAGE}:${imageTag} \
                                -t ${DOCKER_IMAGE}:latest \
                                --push .
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy to Test Server') {
            steps {
                sshagent(credentials: ['ssh-develop-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -p 11022 ${SSH_USER}@${TEST_SERVER_IP} '
                            # 1. 프로젝트 디렉토리 이동
                            cd /home/${SSH_USER}/MOUP-Server

                            # 2. 최신 이미지 받기
                            docker compose pull server
                            
                            # 3. 컨테이너 재시작
                            docker compose up -d server

                            # 4. 불필요한 이미지 정리
                            docker image prune -f

                            # 5. Crontab 설정 (사용자 삭제 스크립트)
                            # echo "Setting up crontab..."
                            # DELETE_SCRIPT="/home/${{ secrets.RPI_USER }}/MOUP-Server/src/main/resources/delete_old_users.sh"
                            # chmod +x "$DELETE_SCRIPT" # 실행 권한 부여
                            # # 기존 스케줄 삭제 후 새로 등록 (멱등성 확보)
                            # (crontab -l 2>/dev/null | grep -v "delete_old_users.sh"; echo "0 0 * * * /bin/bash $DELETE_SCRIPT >> /home/${{ secrets.RPI_USER }}/MOUP-Server/delete_old_users.log 2>&1") | crontab -
                            # echo "Crontab setup complete."
                        '
                    """
                }
            }
        }
    }
}
