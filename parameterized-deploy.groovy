pipeline {
    agent any
    options {
        ansiColor('xterm')
        disableConcurrentBuilds()
    }
    parameters {
        /* More parameter types described here: https://jenkins.io/doc/book/pipeline/syntax/#parameters */
    }
    environment {
        // Directory to be used to clone the project's code into
        PROJECT_DIR = "${env.WORKSPACE}/<YOUR_PROJECT_HERE>"
        // Directory to clone the Jenkins standard library into
        STDLIB_DIR = "${env.WORKSPACE}/gcp-jenkins-stdlib"
        // Set deploy environment for Terraform - 'infra/environments/ENV' is the standard structure
        DEPLOY_ENV = "${env.PROJECT_DIR}/infra/environments/${env.GITLAB_BRANCH}"

        /* Fetch correct Service Account credentials and export for use by Terraform
           - Place the Service Account email in the credentials() method, and use ${env.GITLAB_BRANCH} to target specific GCP Projects.
           - Remember, GCP projects are named "marketo-PROJECT-(dev|int|qe|st|prod)"
           - Example: 'project-service-account@marketo-mpc-identity-${env.GITLAB_BRANCH}.iam.gserviceaccount.com' */
        GOOGLE_APPLICATION_CREDENTIALS = credentials("SERVICE_ACCOUNT@PROJECT-${env.GITLAB_BRANCH}.iam.gserviceaccount.com")
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
                /* Grab the infrastructure code from the branch which triggered this deployment
                   - Insert your GitLab project URL in the appropriate field
                   - Keep 'mgmt' as the credentials used, it has access to all projects */
                dir ("${env.PROJECT_DIR}"){
                    checkout scm: [
                        $class: 'GitSCM', userRemoteConfigs: [
                            [
                                url: 'git@gitlab.marketo.org:YOUR_PROJECT_HERE.git',
                                credentialsId: 'mgmt',
                                changelog: false,
                            ]
                        ],
                        branches: [
                            [
                                name: "${env.GITLAB_BRANCH}"
                            ]
                        ],
                        poll: false
                    ]
                }
                dir ("${env.STDLIB_DIR}"){
                    checkout scm: [
                        $class: 'GitSCM', userRemoteConfigs: [
                            [
                                url: 'git@gitlab.marketo.org:GCP-Infrastructure/gcp-jenkins-stdlib.git',
                                credentialsId: 'mgmt',
                                changelog: false,
                            ]
                        ],
                        branches: [
                            [
                                // Update this to use the latest tagged version of the stdlib
                                name: "refs/tags/v0.1.0"
                            ]
                        ],
                        poll: false
                    ]
                }
            }
        }
        stage('Deploy BE') {
            steps {
                echo "Deploying Backend: $DEPLOY_ENV/be"
                // Run the following steps from the DEPLOY_ENV directory
                dir ("$DEPLOY_ENV/be") {
                    // Run Terraform init with via the shell command
                    sh "${env.STDLIB_DIR}/apply.sh -p addresses.tfplan -s ${env.GOOGLE_APPLICATION_CREDENTIALS}"
                }
            }
        }
        stage('Deploy FE') {
            steps {
                echo "Deploying Backend: $DEPLOY_ENV/fe"
                dir ("$DEPLOY_ENV/fe") {
                    sh "${env.STDLIB_DIR}/apply.sh -p addresses.tfplan -s ${env.GOOGLE_APPLICATION_CREDENTIALS}"
                }
            }
        }
    }
    // Post describes the steps to take when the pipeline finishes
    post {
        //changed {}
        //aborted {}
        //failure {}
        //success {}
        //unstable {}
        //notBuilt {}
        always {
            echo "Clearing workspace"
            deleteDir() // Clean up the local workspace so we don't leave behind a mess, or sensitive files
        }
    }
}
