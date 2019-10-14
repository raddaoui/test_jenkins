/*
 * Copyright 2018 Google LLC. This software is provided as-is,
 * without warranty or representation for any use or purpose.
 * Your use of it is subject to your agreements with Google.
 */

pipeline {
    /*
     * This pipeline will run with the label given to 'agent'.
     * Labels can be assigned to Jenkins agents to separate unique build environments, e.g. 'centos', 'ubuntu'.
     * 'any' will use any available agent at run time.
     */
    agent any
    /*
     * 'options' change the behavior of the whole pipeline.
     * See the Jenkins documentation for available options.
     */
    options {
        // Converts color codes into color in the build output
        ansiColor('xterm')

        // Prevent more than one of this pipeline from running at once.
        disableConcurrentBuilds()
    }
    /*
     * 'environment' supports variable assignment and automatically exports them to the environment.
     */
    environment {
        // You can reference this with "${env.FOO}"
        FOO = "BAR"

        /* 
         * You can assign secrets/credentials to variables with the following method.
         * This secret will be shown as '*****' in build logs, or if you try to print them.
         * The actual secret is used internally as necessary.
         */ 
        MY_CREDS = credentials('my-creds-id')
    }
    /*
     * Parameters are available to the user in the Jenkins UI when executing a build
     * You can reference these by "${params.NAME}"
     * Types of parameters: https://jenkins.io/doc/book/pipeline/syntax/#parameters
     */
    parameters {
        // The name of this parameter is 'person', reference it with "${params.person}"
        string(name: 'person', defaultValue: 'World', description: 'The person you want to greet.') 
    }
    stages {
        /* 
         * Each stage will appear as a separate block in the pipeline execution screen.
         * Each stage must be given a name, e.g. stage('Foo').
         * You can have as many or few stages as you need. They should represent logical groupings of work.
         */
        stage('echo and sh demo') {
            /*
             * All commands must be inside the steps block
             */
            steps {
                // You can echo basic text into the build output
                echo 'Hello, World!'

                // The 'sh' command will run the shell command from inside the quotes
                sh "echo 'Hello, World!'"

                // You can interpolate variables with ${} syntax. Outside single quotes will NOT allow interpolation.
                sh "echo 'Hello, ${params.person}!'" // --> "Hello, World!"
                sh 'echo "Hello, ${params.person}!"' // --> "Hello, ${params.person}!"

                // You can execute scripts
                sh "hello.sh ${params.person}"

                // You can run commands from different directories of your workspace with dir()
                dir("${env.WORKSPACE}/some-folder") {
                    sh "pwd"
                }
            }
        }
        stage('script demo') {
            steps {
                /*
                 * Groovy scripting must go in the script block
                 * You can use pipeline-specific declarative methods inside 'script' as well, e.g. credentials()
                 */
                script {
                    if (params.person != 'Nick') {
                        println 'I hope this generic pipeline was helpful!'
                    {
                    println "${credentials('super-secret')}" // --> "*****"
                }
            }
        }
        stage('git demo') {
            /*
             * This will clone the desired branch from GitLab and place it into ${env.WORKSPACE}
             * 'mgmt' is a global credential and should always be used for Marketo's GitLab
             * You can use variable interpolation for the branch name, e.g. "${params.branch}"
             */
            git branch: "BRANCH_NAME", changelog: false, credentialsId: 'mgmt', poll: false, url: 'git@gitlab.marketo.org:YOUR_PROJECT.git'
        }
    }
    /*
     * 'post' describes the steps to take when the pipeline finishes depending on the build status
     */
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

