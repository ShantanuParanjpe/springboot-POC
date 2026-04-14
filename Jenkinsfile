pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE_NAME = 'springboot-demo'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ShantanuParanjpe/springboot-POC.git'
            }
        }

        stage('Build & SonarQube Scan') {
            agent {
                docker {
                    image 'maven:3.9.11-eclipse-temurin-17'
                    reuseNode true
                }
            }
            steps {
                dir('springboot/demo') {
                    withSonarQubeEnv('sonarqube') {
                        sh '''
                            mvn clean verify sonar:sonar \
                              -Dsonar.projectKey=springboot-demo \
                              -Dsonar.projectName=springboot-demo
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('springboot/demo') {
                    sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
                }
            }
        }

        stage('Trivy Scan') {
            agent {
                docker {
                    image 'aquasec/trivy:0.69.3'
                    reuseNode true
                    args "-v /var/run/docker.sock:/var/run/docker.sock -v /tmp/trivy-cache:/root/.cache --group-add $(stat -c '%g' /var/run/docker.sock) --entrypoint=''"
                }
            }
            steps {
                sh '''
                    trivy image \
                      --cache-dir /tmp/trivy-cache \
                      --exit-code 1 \
                      --severity HIGH,CRITICAL \
                      ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-token',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} $DOCKER_USER/${IMAGE_NAME}:latest

                        docker push $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push $DOCKER_USER/${IMAGE_NAME}:latest
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
    }
}