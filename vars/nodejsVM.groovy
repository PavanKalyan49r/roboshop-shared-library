def call(Map configMap){
pipeline {
    agent {
        node{
            label 'AGENT-1'
        }
    }
    environment {
       packageVersion =''
    //    can maintain in pipeline globals
    //    nexusUrl = '172.31.18.231:8081'
    }
    options {
        timeout(time:1, unit: 'HOURS')
        disableConcurrentBuilds()
    }
    parameters {
    //     string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

    //     text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

        booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')

        // choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

        // password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    } 
// build
    stages {
        stage('get the version') {
            steps {
                script{
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version
                    echo "application version: $packageVersion"
                }
            }
        }
        stage('install dependencies') {
            steps {
                sh """
                   npm install
                """
            }
        }
        stage('unit tests') {
            steps {
                sh """
                   echo "unit tests will run here"
                """
            }
        }
        stage('sonar scan') {
            steps {
                sh """
                   sonar-scanner
                """
            }
        }
        stage('build') {
            steps {
                sh """
                ls -la
                zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                ls -ltr
                """
            }
        }
        stage('publish artifact') {
            steps {
                nexusArtifactUploader(
                   nexusVersion: 'nexus3',
                   protocol: 'http',
                   nexusUrl: pipelineGlobals.nexusURL(),
                   groupId: 'com.roboshop',
                   version: "${packageVersion}",
                   repository: "${configMap.component}",
                   credentialsId: 'nexus-auth',
                     artifacts: [
                            [artifactId: "${configMap.component}",
                            classifier: '',
                            file: "${configMap.component}.zip",
                            type: 'zip']
                                ]
                )
            }
        }
        stage('Deploy') {
            when {
                expression{
                    params.Deploy == true
                }
            }
            steps {
                script{
                    def params = [
                        string(name: 'version',value: "${packageVersion}"),
                        string(name: 'environment', value: "dev")
                    ]
                build job: "${configMap.component}-deploy", wait: true, parameters: params
                }
            }
        }
        
    }
    //post build
    post {
        always{
            echo 'i will always say hello again'
            deleteDir()
        }
        failure{
            echo 'this runs when pipeline is failed, used generally to send some alrets'
        }
        success{
            echo 'i will say hello when pipeline is success'
        }
    }
}
}