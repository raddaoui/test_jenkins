/*
 * Copyright 2018 Google LLC. This software is provided as-is,
 * without warranty or representation for any use or purpose.
 * Your use of it is subject to your agreements with Google.
 */

pipeline {
    agent any
    options {
        // Apply a timeout to the entire pipeline
        timeout(time: 1, unit: 'HOURS')
    }
    stages {
        // Apply a timeout on a whole stage
        stage('Timeout on stage') {
            options {
                timeout(time: 1, unit: 'MINUTES') 
            }
            steps {
                sh 'sleep 5'
            }
        }
        stage('Timeout on step') {
            steps {
                // Apply a timeout on a single step
                timeout(time: 10, unit: 'SECONDS') {
                    sh 'sleep 5'
                }
            }
        }
    }
}
