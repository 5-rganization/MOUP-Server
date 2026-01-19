pipeline {
    agent any

    environment {
        // 공통 설정
        DOCKER_IMAGE = "neoskycladdocker/moup"
        TEST_SERVER_IP = "test.moup-server.com"
        SSH_USER = "moup-server"
        
        // 단일 파이프라인에서는 env.BRANCH_NAME이 자동으로 안 잡힐 수 있습니다.
        // develop 전용이므로 직접 명시하거나, GIT_BRANCH 변수를 가공해야 합니다.
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
                    sh 'cp $ENV_FILE .env'
                }
            }
        }

        stage('Build Gradle') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('Build & Push Docker') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-auth') {
                        // 태그: develop-빌드번호
                        def imageTag = "${TARGET_BRANCH}-${env.BUILD_NUMBER}"
                        def customImage = docker.build("${DOCKER_IMAGE}:${imageTag}")
                        customImage.push()
                        
                        // develop 브랜치는 latest 태그도 업데이트 (선택사항)
                        customImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Test Server') {
            steps {
                sshagent(credentials: ['ssh-deploy-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${SSH_USER}@${TEST_SERVER_IP} '
                            docker pull ${DOCKER_IMAGE}:${TARGET_BRANCH}-${env.BUILD_NUMBER}
                            docker stop moup-server || true
                            docker rm moup-server || true
                            
                            docker run -d --name moup-server \
                            -p 8080:8080 \
                            ${DOCKER_IMAGE}:${TARGET_BRANCH}-${env.BUILD_NUMBER}
                        '
                    """
                }
            }
        }
    }
}
