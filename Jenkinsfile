pipeline {
    agent {
            docker {
                image 'maven:3.8.5-openjdk-11'
                args '-v /var/run/docker.sock:/var/run/docker.sock'
            }
    }
    environment {
        DOCKER_BUILDKIT = 1
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/pa-mi-su/secure-springboot-microservice.git'
            }
        }

        stage('Build JARs') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker build -t userms-app -f userms/Dockerfile ./userms'
                sh 'docker build -t notificationms-app -f notificationms/Dockerfile ./notificationms'
            }
        }

        stage('Docker Compose Up') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        failure {
            echo 'Build or deployment failed!'
        }
    }
}
