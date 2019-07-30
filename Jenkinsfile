node('socle-jenkins-maven-docker-14-4G') {

  checkout scm

  try{
    env.JAVA_HOME = "${variables.jdkPathPrefix}1.8.0_172"
    sh 'mvn -B clean install'
    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml,**/failsafe-reports/*.xml'
  } catch (Exception e) {
    throw e
  }
}
return this;
