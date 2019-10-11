pipeline {
    agent any
    options {
        //ansiColor('xterm')
        disableConcurrentBuilds()
    }
    parameters {
        /* More parameter types described here: https://jenkins.io/doc/book/pipeline/syntax/#parameters */
        booleanParam(name: 'refresh', defaultValue: true, description: 'Refresh job config to update UI from Pipeline')
        choice(name: 'action', choices: ['apply', 'delete'], description: 'action to apply or delete kubernetes deployment')
        string(name: 'CLUSTER_NAME', defaultValue: '', description: 'kubernetes cluster to deploy to')
    }
    environment {
        // Directory to be used to clone the project's code into
        PROJECT_DIR = "${env.WORKSPACE}/test_jenkins"
        // Set deploy environment for Terraform - 'infra/environments/ENV' is the standard structure
        DEPLOY_ENV = "${env.PROJECT_DIR}/helm"
        GITHUB_BRANCH = "master"
        APPROVERS = 'ala'
    }
    stages {
        stage('Init') {
            steps {
                script {
                    if (params.refresh) {
                        // Simply abort the job early if we want to refresh the UI elements of job config after initial import or changes
                        currentBuild.result = 'ABORTED'
                        error('Refreshing job configuration from Pipeline DSL.')
                    }
                }
                withCredentials([file(credentialsId: 'gcp-jenkins', variable: 'GC_KEY')]) {
                    sh("gcloud auth activate-service-account --key-file=${GC_KEY}")
                }
                sh("gcloud container clusters get-credentials jenkins-test --zone us-central1-a --project sada-ala-radaoui")
                /* Grab the infrastructure code from the branch which triggered this deployment
                   - Insert your GitLab project URL in the appropriate field
                   - Keep 'mgmt' as the credentials used, it has access to all projects */
                dir ("${env.PROJECT_DIR}"){
                    checkout scm: [
                        $class: 'GitSCM', userRemoteConfigs: [
                            [
                                url: 'git@github.com:raddaoui/test_jenkins.git',
                                credentialsId: 'github-raddaoui',
                                changelog: false,
                            ]
                        ],
                        branches: [
                            [
                                name: "${env.GITHUB_BRANCH}"
                            ]
                        ],
                        poll: false
                    ]
                }
            }
        }
        stage('Deploy helm') {
            steps {
                echo "Deploying helm chart: $DEPLOY_ENV"
                // Run the following steps from the DEPLOY_ENV directory
                dir ("$DEPLOY_ENV") {
                    // Run Terraform init with via the shell command
                    //sh("gcloud container clusters list --project sada-ala-radaoui")
                    input(message: "\nContinue with action: ${params.action}?\n", submitter: "${env.APPROVERS}")
                    sh("helm template -n iap-connector ./iap-connector/ -f values/${CLUSTER_NAME}_values.yaml | kubectl ${params.action} -f- ") 
                }
            }
        }
    }
    // Post describes the steps to take when the pipeline finishes
    post {
        always {
            echo "Clearing workspace"
            sh "gcloud auth revoke"
            deleteDir() // Clean up the local workspace so we don't leave behind a mess, or sensitive files
        }
    }
}
