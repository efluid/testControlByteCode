import com.efluid.jenkinsfile.Jenkinsfile

// variables globales
node('socle-jenkins-maven-docker-14-4G') {

  checkout scm

  try{
    sh 'mvn clean install'
    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml,**/failsafe-reports/*.xml'
  } catch (Exception e) {
    //TODO send mail
    throw e
  }
}
return this;
