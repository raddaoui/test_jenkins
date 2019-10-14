/*
 * Copyright 2018 Google LLC. This software is provided as-is,
 * without warranty or representation for any use or purpose.
 * Your use of it is subject to your agreements with Google.
 */

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
        DEPLOY_ENV = "infra/environments/${env.GITLAB_BRANCH}/frontend"
        // Replace "PROJECT" with your GCP Project's prefix, e.g. "marketo-mpc-locator"
        GOOGLE_APPLICATION_CREDENTIALS = credentials("project-service-account@PROJECT-${env.GITLAB_BRANCH}.iam.gserviceaccount.com")
        // CSV of approvers for this job - must be local Jenkins users, LDAP users, or LDAP groups
        APPROVERS = 'RelEng,REPO-ENG'
    }
    stages {
        stage('Code checkout') {
            steps {
                // Checkout the infrastructure code from the branch which triggered this deployment
                // - Insert your GitLab project URL in the appropriate field
                // - Keep 'mgmt' as the credentials used, it has access to all projects
                git branch: "${env.GITLAB_BRANCH}", changelog: false, credentialsId: 'mgmt', poll: false, url: 'git@gitlab.marketo.org:YOUR_GITLAB_PROJECT.git'
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
        stage('Redeploy uMIG resources') {
            steps {
                dir (DEPLOY_ENV) {
                    script {
                        for (resource in umig) {
                            try {
                                // `terraform taint` uses different resource addressing than `plan` and `apply`
                                transformed_resource = resource.replace('[', '.').replace(']', '').replace('module.', '')
                                module_hierarchy = transformed_resource.tokenize('.')[0..-4].join('.')
                                taint_resource = transformed_resource.tokenize('.')[-3..-1].join('.')
                            } catch (Error err) {
                                println err
                                println "Check your version of terraform to see if resource addressing for taint has changed"
                            }

                            println "\n\n\n\n\n=====================\n=== Next Resource ===\n====================="
                            sh "terraform taint -module=${module_hierarchy} ${taint_resource}"
                            sh "terraform plan -input=false -target=${resource} -out=tfplan"
                            input(message: "\nContinue with above redeployment?\n", submitter: "${env.APPROVERS}")
                            sh "terraform apply tfplan"
                        }
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
