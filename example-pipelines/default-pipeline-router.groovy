/*
 * Copyright 2018 Google LLC. This software is provided as-is,
 * without warranty or representation for any use or purpose.
 * Your use of it is subject to your agreements with Google.
 */

/*
 * Use this job to call other pipelines using variable interpolation
 * This is useful if you want to automatically run environment-specific pipelines
 * on GitLab push-event webhooks but don't want to call every environment at once. 
 * 
 * Name this file starting with 'default-' so that only one version is imported
 * instead of one per branch.
 * 
 * You can remove the `parameters: ...` from `build job: ...` if your downstream
 * pipeline does not require any.
 */

pipeline {
    agent any
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: false, triggerOnNoteRequest: false, branchFilterType: 'All')
    }
    stages {
        stage('Route') {
            steps {
                script {
                    try {
                        build job: "some-jenkins-folder/deploy-some-app-${env.gitlabSourceBranch}", parameters: [string(name: 'foo', value:'bar')], wait: false
                    } catch (Exception err) {
                        println "Job does not exist"
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
        }
    }
}
