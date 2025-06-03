pipeline {
      agent {
        kubernetes {
          yamlFile 'pod-template.yaml'
          defaultContainer 'docker-cli'
        }
      }

      parameters {
          string(name: 'CONTAINER_REGISTRY', defaultValue: 'container-registrytechostartup.center', description: 'Container Registry URL to push the Image')
          string(name: 'CONTAINER_REGISTRY_PROJECT_NAME', defaultValue: 'camdx', description: 'Project path to push the Image')
      }

    environment {
        IMAGE_NAME = 'xroad-security-server'
        DOCKER_REGISTRY = 'container-registry-dev.techostartup.center' // Optional: for pushing image
        FULL_IMAGE_NAME = "${DOCKER_REGISTRY}/${CONTAINER_REGISTRY_PROJECT_NAME}/${IMAGE_NAME}:latest"
    }

    stages {
         stage('Docker Login') {
             steps {
               container('docker-cli') {
                 withEnv(["DOCKER_HOST=unix:///var/run/docker.sock"]) {
                   withCredentials([
                     usernamePassword(
                       credentialsId: 'container-registry-jenkins',
                       usernameVariable: 'DOCKER_USERNAME',
                       passwordVariable: 'DOCKER_PASSWORD'
                     )
                   ]) {
                     sh '''
                       echo ${DOCKER_PASSWORD} | docker login ${CONTAINER_REGISTRY} -u ${DOCKER_USERNAME} --password-stdin
                     '''
                   }
                 }
               }
             }
           }

           stage('Build X-Road Packages using Docker') {
               steps {
                 container('docker-cli') {
                   withEnv(["DOCKER_HOST=unix:///var/run/docker.sock"]) {
                     sh '''
                       echo "Starting X-Road build using Docker..."
                         chmod +x build_packages.sh
                         ./src/build_packages.sh -d
                     '''
                   }
                 }
               }
             }

           stage('Push Docker Image') {
             steps {
               container('docker-cli') {
                 withEnv(["DOCKER_HOST=unix:///var/run/docker.sock"]) {
                     sh """
                          echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                          docker push ${FULL_IMAGE_NAME}
                      """
                 }
               }
             }
           }

    }

    post {
        success {
            echo "✅ Build and packaging completed successfully."
        }
        failure {
            echo "❌ Build or packaging failed."
        }
    }
}
