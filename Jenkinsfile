import com.efluid.jenkins.*

JenkinsUtils jenkinsUtils = new JenkinsUtils(this)

def body = {

  checkout scm

  try{
    env.JAVA_HOME = "${variables.jdkPathPrefix}1.8.0_172"
    sh 'mvn -B clean install'
    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml,**/failsafe-reports/*.xml'
  } catch (Exception e) {
    throw e
  }
}

if (jenkinsUtils.isCjeProd()) {
    node('socle-jenkins-maven-docker-14-4G') {
        body.call()
    }
} else {
    new EfluidPodTemplate(this).addContainer(new Container(this).containerType("maven-14").memory("4G")).execute() {
        container("maven-14") {
            body.call()
        }
    }
}
return this;
