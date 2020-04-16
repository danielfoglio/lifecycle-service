package io.cratekube.lifecycle.service

import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.ProcessExecutor
import io.cratekube.lifecycle.api.exception.FailedException
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static org.hamcrest.Matchers.startsWith
import static spock.util.matcher.HamcrestSupport.expect

class KubectlServiceSpec extends Specification {
  @Subject KubectlApi subject
  ProcessExecutor kubectlCmd
  AppConfig config

  def setup() {
    kubectlCmd = Mock()
    config = new AppConfig(kubeconfigLocation: '/test/location')
    subject = new KubectlService(kubectlCmd, config)
  }

  def 'should require valid constructor params'() {
    when:
    new KubectlService(cmd, conf)

    then:
    thrown RequireViolation

    where:
    cmd             | conf
    null            | null
    this.kubectlCmd | null
  }
  def 'apply should require valid args'() {
    when:
    subject.apply(yaml)

    then:
    thrown RequireViolation

    where:
    yaml << [null, '']
  }

  def 'apply should successfully execute an apply command'() {
    given:
    def yaml = 'deployable k8s yaml'

    when:
    subject.apply(yaml)

    then:

    1 * kubectlCmd.exec(startsWith("--kubeconfig ${config.kubeconfigLocation} apply -f")) >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as PrintStream, _ as PrintStream)
      1 * exitValue() >> 0
    }
  }

  def 'apply should throw FailedException when exec results in error'() {
    given:
    def yaml = 'bad k8s yaml'

    when:
    subject.apply(yaml)

    then:
    1 * kubectlCmd.exec(startsWith("--kubeconfig ${config.kubeconfigLocation} apply -f")) >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as PrintStream, _ as PrintStream)
      1 * exitValue() >> 1
    }
    thrown FailedException
  }

  def 'get should require valid args'() {
    when:
    subject.getPodJsonByNameSelector(args)

    then:
    thrown RequireViolation

    where:
    args << [null, '']
  }

  def 'get should retrieve the kubernetes resource'() {
    given:
    def podName = 'test-deployment'

    when:
    def result = subject.getPodJsonByNameSelector(podName)

    then:
    kubectlCmd.exec("--kubeconfig ${config.kubeconfigLocation} get pod -l name=${podName} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as StringBuffer, _ as StringBuffer) >> { out, err ->
        out.append 'not empty'
      }
      1 * exitValue() >> 0
    }
    expect result, notEmptyString()
  }

  def 'get should throw FailedException when get results in error'() {
    given:
    def podName = 'test-deployment'

    when:
    subject.getPodJsonByNameSelector(podName)

    then:
    kubectlCmd.exec("--kubeconfig ${config.kubeconfigLocation} get pod -l name=${podName} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as StringBuffer, _ as StringBuffer)
      1 * exitValue() >> 1
    }
    thrown FailedException
  }
}
