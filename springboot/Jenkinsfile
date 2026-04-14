pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE_NAME = 'springboot-demo'
        IMAGE_TAG = 'latest'
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
                    sh 'docker build -t springboot-demo:latest .'
                }
            }
        }
    }

 }
  