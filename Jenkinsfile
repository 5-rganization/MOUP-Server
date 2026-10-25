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

        // 예전 'Prepare Env' 스테이지는 Firebase 서비스 계정 키를 src/main/resources/keys/에 복사했다.
        // 키가 jar에 들어가 public Docker 이미지로 push됐다(스코프 7 C7). 이제 키는 각 서버의
        // secrets/firebase-key.json에 두고 compose가 마운트한다. 같은 스테이지의 .env 복사도
        // 이미지에 들어가지 않는 죽은 단계였고, Dockerfile을 `COPY .`로 바꾸는 순간 시크릿이 새는 함정이라 뺐다.
        stage('Build Gradle') {
            steps {
                dir('server') {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build -x test'   // TODO: 추후에 테스트 포함하기 -> CI 구현
                }
            }
        }

        stage('Build & Push Docker') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-auth', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        dir('server') {
                            sh 'printf "%s" "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'  
                            
                            // [핵심] 브랜치에 따라 태그 전략 다르게 가져가기
                            // develop -> develop-빌드번호 (latest)
                            // main    -> release-빌드번호 (stable)
                            def branchName = env.BRANCH_NAME
                            def safeBranchName = branchName.replaceAll("/", "-")
                            def buildNum = env.BUILD_NUMBER
                            
                            def versionTag = "${safeBranchName}-${buildNum}"
                            def aliasTag = (branchName == 'main') ? 'stable' : 'latest'
                            
                            // 환경변수로 내보내기 (Deploy 스테이지에서 쓰기 위함)
                            env.VERSION_TAG = versionTag

                            sh """
                                docker buildx use moup-builder || docker buildx create --name moup-builder --use
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
