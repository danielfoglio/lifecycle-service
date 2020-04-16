package io.cratekube.lifecycle.service

import groovy.util.logging.Slf4j
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.ProcessExecutor
import io.cratekube.lifecycle.api.exception.FailedException

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Slf4j
class KubectlService implements KubectlApi {
  ProcessExecutor kubectlCmd
  AppConfig config

  @Inject
  KubectlService(ProcessExecutor kubectlCmd, AppConfig config) {
    this.kubectlCmd = require kubectlCmd, notNullValue()
    this.config = require config, notNullValue()
  }

  @Override
  void apply(String yaml) throws FailedException {
    require yaml, notEmptyString()

    def deployment = File.createTempFile('tmp', '.yml') << yaml
    def proc = kubectlCmd.exec("--kubeconfig ${config.kubeconfigLocation} apply -f ${deployment.path}")
    proc.waitForProcessOutput(System.out, System.err)
    if (proc.exitValue() != 0) {
      throw new FailedException("Error executing apply for yaml [${yaml}]")
    }
  }

  @Override
  String getPodJsonByNameSelector(String name) throws FailedException {
    require name, notEmptyString()

    def proc = kubectlCmd.exec("--kubeconfig ${config.kubeconfigLocation} get pod -l name=${name} -o json")
    def (out, err) = [new StringBuffer(), new StringBuffer()]
    proc.waitForProcessOutput(out, err)
    def procOutput = out + err
    log.debug procOutput
    if (proc.exitValue() != 0) {
      throw new FailedException("Error executing get pod for selector name=${name}]")
    }
    return procOutput
  }
}
