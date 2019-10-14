pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    parameters {
        /* More parameter types described here: https://jenkins.io/doc/book/pipeline/syntax/#parameters */
        booleanParam(name: 'refresh', defaultValue: true, description: 'Refresh job config to update UI from Pipeline')
        choice(name: 'action', choices: ['apply', 'delete'], description: 'action to apply or delete kubernetes deployment')
        string(name: 'cluster_name', defaultValue: '', description: 'kubernetes cluster to deploy to')
        string(name: 'cluster_region', defaultValue: '', description: 'kubernetes cluster region')
    }
    environment {
        // Directory to be used to clone the project's code into
        PROJECT_DIR = "${env.WORKSPACE}/test_jenkins"
        // Set deploy environment for helm
        DEPLOY_ENV = "${env.PROJECT_DIR}/helm"
        GCP_CREDS_ID = "gcp-jenkins"
        GITHUB_CREDS_ID = "github-raddaoui"
        PROJECT_ID = "sada-ala-radaoui"
        GITHUB_BRANCH = "master"
        // CSV of approvers for this job - must be local Jenkins users, LDAP users, or LDAP groups
        APPROVERS = "ala"
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
                withCredentials([file(credentialsId: "${env.GCP_CREDS_ID}", variable: 'GC_KEY')]) {
                    sh("gcloud auth activate-service-account --key-file=${GC_KEY}")
                }
                sh("gcloud container clusters get-credentials ${params.cluster_name} --zone ${params.cluster_region} --project ${params.cluster_id}")
                dir ("${env.PROJECT_DIR}"){
                    checkout scm: [
                        $class: 'GitSCM', userRemoteConfigs: [
                            [
                                url: 'git@github.com:raddaoui/test_jenkins.git',
                                credentialsId: "${env.GITHUB_CREDS_ID}",
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
                    input(message: "\nContinue with action: ${params.action}?\n", submitter: "${env.APPROVERS}")
                    sh("helm template -n iap-connector ./iap-connector/ -f values/${params.cluster_name}_values.yaml | kubectl ${params.action} -f- ")
                }
            }
        }
    }
    // Post describes the steps to take when the pipeline finishes
    post {
        //changed {}
        //aborted {}
        //failure {}
        success {
            echo "Apply complete!"
        }
        //unstable {}
        //notBuilt {}
        always {
            echo "Clearing workspace"
            sh "gcloud auth revoke"
            deleteDir() // Clean up the local workspace so we don't leave behind a mess, or sensitive files
        }
    }
}
