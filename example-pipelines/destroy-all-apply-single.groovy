/*
 * Copyright 2018 Google LLC. This software is provided as-is,
 * without warranty or representation for any use or purpose.
 * Your use of it is subject to your agreements with Google.
 */

/* This pipeline destroys all instances in a group and restores them one-by-one.
   It is potentially useful for version-incompatible redeployments. */
pipeline {
    agent any
    options {
        /* Adds color to terminal text - helpful for reading Terraform output
          This will activate red and green text - turn this off for vision accessibility reasons. */
        ansiColor('xterm')
        // Prevent more than one of this pipeline from running at once
        disableConcurrentBuilds()
    }
    environment {
        DEPLOY_ENV = "infra/environments/${env.GITLAB_BRANCH}/fooapp-fe"
        // Replace PROJECT_ID_PREFIX with your GCP Project ID without the environment
        GOOGLE_APPLICATION_CREDENTIALS = credentials("project-service-account@PROJECT_ID_PREFIX-${env.GITLAB_BRANCH}.iam.gserviceaccount.com")
        // CSV of local Jenkins users or LDAP groups which are allowed to approve deployments
        APPROVERS = 'RelEng'
    }
    stages {
        stage('Code checkout') {
            steps {
                // Checkout the infrastructure code from the branch which triggered this deployment
                // - Insert your GitLab project URL in the appropriate field
                // - Keep 'mgmt' as the credentials used, it has access to all projects
                git branch: "${env.GITLAB_BRANCH}", changelog: false, credentialsId: 'mgmt', poll: false, url: 'git@gitlab.marketo.org:YOUR_PROJECT.git'
            }
        }
        stage('Identify uMIG resources') {
            steps {
                dir (DEPLOY_ENV) {
                    script {
                        // Initialize Terraform so that state can be fetched from remote
                        sh 'terraform init'
                        // List the resource types used in the current state and filter out everything but GCE instances
                        cmd = "terraform state list | grep 'google_compute_instance\\.'"
                        umig = sh (script: cmd, returnStdout: true).trim().tokenize("\n")
                    }
                }
            }
        }
        stage('Destroy uMIG resources') {
            steps {
                dir (DEPLOY_ENV) {
                    script {
                        // Reference the uMIG without indices
                        group_resource = umig[0].tokenize('[')[0]
                        sh "terraform plan -destroy -target=${group_resource} -out=destroy.tfplan"
                        input(message: "\nContinue with above destroy?\n", submitter: "${env.APPROVERS}")
                        sh "terraform apply destroy.tfplan"
                    }
                }
            }
        }
        stage('Deploy uMIG resources') {
            steps {
                dir (DEPLOY_ENV) {
                    script {
                        for (resource in umig) {
                            println "\n\n\n\n\n=====================\n=== Next Resource ===\n====================="
                            sh "terraform plan -input=false -target=${resource} -out=tfplan"
                            input(message: "\nContinue with the above deployment?\n", submitter: "${env.APPROVERS}")
                            sh 'terraform apply tfplan'
                        }
                    }
                }
            }
        }
        stage('Deploy final resources') {
            steps {
                dir (DEPLOY_ENV) {
                    script {
                        println "\n\n\n\n\n=======================\n=== Final Resources ===\n======================="
                        sh 'terraform plan -out=tfplan'
                        input(message: "\nContinue with the above deployment?\n", submitter: "${env.APPROVERS}")
                        sh 'terraform apply tfplan'
                    }
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
            echo "Deployment complete!"
        }
        //unstable {}
        //notBuilt {}
        always {
            echo "Clearing workspace..."
            deleteDir() // Clean up the local workspace so we don't leave behind a mess, or sensitive files
        }
    }
}
