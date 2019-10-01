pipeline {
    agent any
    environment {
        PROJECT_ID = 'host-iap-raddaoui'
        CLUSTER_NAME = 'standard-cluster-1'
        LOCATION = 'us-central1-a'
        CREDENTIALS_ID = 'sada-ala-radaoui'
    }
    stages {
        stage('Deploy to GKE') {
            steps{
                step([
                $class: 'KubernetesEngineBuilder',
                projectId: env.PROJECT_ID,
                clusterName: env.CLUSTER_NAME,
                location: env.LOCATION,
                manifestPattern: 'manifest.yaml',
                credentialsId: env.CREDENTIALS_ID,
                verifyDeployments: true])
            }
        }
    }
}
