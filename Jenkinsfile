pipeline {
    agent any

    environment {
        // 1. 공통 설정
        DOCKER_IMAGE = "neoskycladdocker/moup"
        
        // 2. 서버 IP 설정 (실제 IP로 변경하세요)
        TEST_SERVER_IP = "test.moup-server.com"
        PROD_SERVER_IP = "home.moup-server.com"
        
        // 3. SSH 접속 계정
        SSH_USER = "moup-server" 
    }

    stages {
        stage('Checkout') {
            steps {
                // Git 코드 가져오기
                checkout scm
            }
        }

        stage('Prepare Env') {
            steps {
                // Jenkins에 등록한 .env 파일을 가져와서 workspace에 복사
                withCredentials([file(credentialsId: 'moup-env-file', variable: 'ENV_FILE')]) {
                    sh 'cp $ENV_FILE .env'
                }
            }
        }

        stage('Build Gradle') {
            steps {
                // 권한 부여 후 빌드 (테스트 제외 - 시간 단축, 필요시 포함)
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test' 
            }
        }

        stage('Build & Push Docker') {
            steps {
                script {
                    // Docker Hub 로그인 및 이미지 빌드/푸시
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-auth') {
                        // 이미지 태그 생성 (브랜치 이름 + 빌드 번호)
                        def imageTag = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
                        def customImage = docker.build("${DOCKER_IMAGE}:${imageTag}")
                        
                        customImage.push() // 태그 버전 푸시
                        customImage.push('latest') // latest 태그도 갱신
                    }
                }
            }
        }

        stage('Deploy to Test Server') {
            when {
                branch 'develop' // develop 브랜치일 때만 실행
            }
            steps {
                sshagent(credentials: ['ssh-develop-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${SSH_USER}@${TEST_SERVER_IP} '
                            docker pull ${DOCKER_IMAGE}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                            docker stop moup-server || true
                            docker rm moup-server || true
                            
                            # .env 파일은 Jenkins에서 SCP로 보내거나, 서버에 미리 세팅해두는 것이 안전함.
                            # 여기서는 Docker 실행 시 환경변수를 주입하는 예시 (보안상 주의)
                            # 혹은 서버에 있는 .env를 사용: --env-file /home/${SSH_USER}/.env
                            
                            docker run -d --name moup-server \
                            -p 8080:8080 \
                            ${DOCKER_IMAGE}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                        '
                    """
                }
            }
        }

        stage('Deploy to Prod Server') {
            when {
                branch 'main' // main 브랜치일 때만 실행
            }
            steps {
                sshagent(credentials: ['ssh-deploy-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${SSH_USER}@${PROD_SERVER_IP} '
                            docker pull ${DOCKER_IMAGE}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                            # 운영 서버는 중단 시간 최소화를 위해 Blue/Green 배포 등을 고려해야 하지만, 
                            # 일단은 중단 배포(Stop & Start) 방식으로 작성함.
                            docker stop moup-server || true
                            docker rm moup-server || true
                            
                            docker run -d --name moup-server \
                            -p 8080:8080 \
                            ${DOCKER_IMAGE}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                        '
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '배포 성공! 🚀'
        }
        failure {
            echo '배포 실패... ㅠㅠ 로그를 확인하세요.'
        }
    }
}
