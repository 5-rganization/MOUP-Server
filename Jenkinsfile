pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "neoskycladdocker/moup"

        SSH_DEV_USER = "neoskyclad"
        TEST_SERVER_IP = "test.moup-server.com"

        SSH_PROD_USER = "snmac"
        PROD_SERVER_IP = "home.moup-server.com"
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
                withCredentials([file(credentialsId: 'moup-firebase-key', variable: 'FIRE_KEY')]) {
                        // (1) 파일을 넣을 디렉토리가 없으면 생성
                        sh 'mkdir -p ./server/src/main/resources/keys'
                        
                        // (2) Jenkins가 준 비밀 파일을 소스 코드 경로로 복사
                        sh 'cp $FIRE_KEY ./server/src/main/resources/keys/moup-e5326-firebase-adminsdk-fbsvc-6ec6692e3b.json'
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
                            sh 'printf "%s" "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'  
                            
                            // [핵심] 브랜치에 따라 태그 전략 다르게 가져가기
                            // develop -> develop-빌드번호 (latest)
                            // main    -> release-빌드번호 (stable)
                            def branchName = env.BRANCH_NAME
                            def buildNum = env.BUILD_NUMBER
                            
                            def versionTag = "${branchName}-${buildNum}"
                            def aliasTag = (branchName == 'main') ? 'stable' : 'latest'
                            
                            // 환경변수로 내보내기 (Deploy 스테이지에서 쓰기 위함)
                            env.VERSION_TAG = versionTag

                            sh """
                                docker buildx create --use || true
                                docker buildx build --platform linux/arm64,linux/amd64 \
                                -t ${DOCKER_IMAGE}:${versionTag} \
                                -t ${DOCKER_IMAGE}:${aliasTag} \
                                --push .
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy to Test Server') {
            when {
                branch 'develop'  // develop 브랜치일 때만 실행
            }
            steps {
                sshagent(credentials: ['ssh-develop-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -p 11022 ${SSH_DEV_USER}@${TEST_SERVER_IP} '
                            cd /home/${SSH_DEV_USER}/MOUP-Server || exit 1

                            git fetch origin develop
                            git checkout develop
                            git pull origin develop

                            # Dockerfile Build 단계에서 만든 태그 주입
                            export TAG=${env.VERSION_TAG}

                            # 개발용 docker-compose 파일 사용
                            docker compose -f docker-compose.dev.yml pull server
                            docker compose -f docker-compose.dev.yml up -d server
                            docker image prune -f
                        '
                    """
                }
            }
        }

        stage('Deploy to Prod Server') {
            when {
                branch 'main'  // main 브랜치일 때만 실행
            }
            steps {
                // [주의] 운영 서버용 SSH 키 ID가 다를 수 있습니다. 확인 후 변경하세요.
                // 만약 같은 키를 쓴다면 'ssh-develop-key' 그대로 써도 됩니다.
                sshagent(credentials: ['ssh-prod-key']) { 
                    sh """
                        ssh -o StrictHostKeyChecking=no ${SSH_PROD_USER}@${PROD_SERVER_IP} '
                            cd /home/${SSH_PROD_USER}/MOUP-Server || exit 1

                            git fetch origin main
                            git checkout main
                            git pull origin main

                            # Dockerfile Build 단계에서 만든 태그 주입
                            export TAG=${env.VERSION_TAG}

                            # [핵심] 운영용 docker-compose 파일 사용
                            docker compose -f docker-compose.prod.yml pull server
                            docker compose -f docker-compose.prod.yml up -d server
                            docker image prune -f
                        '
                    """
                }
            }
        }
    }
}
