package io.cratekube.lifecycle.job

import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.model.Component
import org.quartz.JobExecutionContext
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

class ComponentDeploymentJobSpec extends Specification {
  @Subject ComponentDeploymentJob subject
  Map<String, Component> componentCache
  ComponentApi componentApi
  AppConfig config

  def setup() {
    componentCache = [:]
    componentApi = Mock()
    config = new AppConfig(managedComponents: [('test-name'): true, ('test-name-2'): false, ('test-name-3'): true])
    subject = new ComponentDeploymentJob(componentCache, componentApi, config)
  }

  def 'should require valid constructor params'() {
    when:
    new ComponentDeploymentJob(cache, comp, conf)

    then:
    thrown RequireViolation

    where:
    cache               | comp              | conf
    null                | null              | null
    this.componentCache | null              | null
    this.componentCache | this.componentApi | null
  }

  def 'doJob should apply components'() {
    given:
    def curVersion = null
    def cfg = 'test-config'
    def latVersion = '1.0.1'

    def nm = 'test-name'
    def component = new Component(nm, cfg, curVersion, latVersion)
    componentApi.getComponent(nm) >> component

    def nm3 = 'test-name-3'
    def component3 = new Component(nm3, cfg, curVersion, latVersion)
    componentApi.getComponent(nm3) >> component3

    when:
    subject.doJob(_ as  JobExecutionContext)

    then:
    2 * componentApi.applyComponent(_, _)
  }

  def 'doJob should continue applying components  on failure'() {
    given:
    def curVersion = null
    def cfg = 'test-config'
    def latVersion = '1.0.1'

    def nm = 'test-name'
    componentApi.getComponent(nm) >> {throw new FailedException()}

    def nm3 = 'test-name-3'
    def component3 = new Component(nm3, cfg, curVersion, latVersion)
    componentApi.getComponent(nm3) >> component3

    when:
    subject.doJob(_ as  JobExecutionContext)

    then:
    1 * componentApi.applyComponent(_, _)
  }
}
