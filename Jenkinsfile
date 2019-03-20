import com.efluid.jenkinsfile.Jenkinsfile

// variables globales
node('socle-jenkins-maven-docker-14-4G') {

  checkout scm

  try{
    sh 'mvn clean install'
    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml,**/failsafe-reports/*.xml'
  } catch (Exception e) {
    mail bcc: '', body: """testControlByteCode en erreur 
	${env.BUILD_URL}""", cc: '', from: 'usinelogicielle@efluid.fr', replyTo: '', subject: 'Test testControlByteCode', to: 'usinelogicielle@efluid.fr'
    throw e
  }
}
return this;
