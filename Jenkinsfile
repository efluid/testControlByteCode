node('socle-jenkins-maven-docker-14-4G') {

  checkout scm

  try{
    env.JAVA_HOME = "${variables.jdkPathPrefix}11.0.1"
    sh 'mvn -B clean install'
    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml,**/failsafe-reports/*.xml'
  } catch (Exception e) {
    //TODO send mail
    throw e
  }
}
return this;
