package io.cratekube.lifecycle.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.model.Component
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue
import static spock.util.matcher.HamcrestSupport.expect

class ComponentServiceSpec extends Specification {
  @Subject ComponentApi subject
  Map<String, Component> componentCache
  KubectlApi kubectlApi
  GitHubApi gitHubApi
  ObjectMapper objectMapper
  AppConfig config

  def setup() {
    componentCache = [:]
    kubectlApi = Mock()
    gitHubApi = Mock()
    objectMapper = new ObjectMapper()
    config = new AppConfig(managedComponents: ['test-name': 'http://test.component.url/releases.atom'])
    subject = new ComponentService(componentCache, kubectlApi, gitHubApi, objectMapper, config)
  }

  def 'should require valid constructor params'() {
    when:
    new ComponentService(cache, kube, git, null, null)

    then:
    thrown RequireViolation

    where:
    cache               | kube            | git
    null                | null            | null
    this.componentCache | null            | null
    this.componentCache | this.kubectlApi | null
  }

  def 'getComponent should require valid args'() {
    when:
    subject.getComponent(name)

    then:
    thrown RequireViolation

    where:
    name << [null, '']
  }

  def 'getComponent should retrieve and return component when not in cache'() {
    given:
    //defaults to empty cache
    def nm = 'test-name'
    def curVersion = '1.0.0'
    def cfg = /{
                  "items": [
                      {
                          "spec": {
                              "containers": [
                                  {
                                      "image": "dockerhub.cisco.com\/crate-docker\/lifecycle-service:${curVersion}"
                                  }
                              ]
                          }
                      }
                  ]
              }/.stripMargin()
    kubectlApi.get(nm) >> cfg
    def latVersion = '1.0.1'
    gitHubApi.getLatestVersionFromAtomFeed(config.managedComponents[nm]) >> latVersion

    when:
    def result = subject.getComponent(nm)

    then:
    expect result, notNullValue()
    expect componentCache, hasEntry(nm, result)
    verifyAll(result) {
      expect name, equalTo(nm)
      expect config, equalTo(cfg)
      expect currentVersion, equalTo(curVersion)
      expect latestVersion, equalTo(latVersion)
    }
  }

  def 'getComponent should return component when in cache'() {
    given:
    def name = 'test-name'
    def config = 'test-config'
    def currentVersion = 'test-current-version'
    def latestVersion = 'test-latest-version'
    def value = new Component(name, config, currentVersion, latestVersion)
    componentCache[name] = value

    when:
    def result = subject.getComponent(name)

    then:
    expect result, notNullValue()
    expect result, equalTo(value)
  }

  def 'getComponent should return null when no resource matches in k8s'() {
    given:
    def name = 'test-name'
    kubectlApi.get(name) >> null

    when:
    def result = subject.getComponent(name)

    then:
    expect result, nullValue()
  }
  def 'applyComponent should require valid args'() {
    when:
    subject.applyComponent(name, version)

    then:
    thrown RequireViolation

    where:
    name           | version
    null           | null
    ''             | null
    'test-version' | null
    'test-version' | ''
  }

  def 'applyComponent should apply component version'() {
    given:
    def nm = 'test-name'
    def ver = 'test-version'
    def depComp = 'deployable-k8s-config'
    gitHubApi.getDeployableComponent(nm, ver) >> depComp
    // needed for updating the cache
    def cfg = /{
                  "items": [
                      {
                          "spec": {
                              "containers": [
                                  {
                                      "image": "dockerhub.cisco.com\/crate-docker\/lifecycle-service:${ver}"
                                  }
                              ]
                          }
                      }
                  ]
              }/.stripMargin()
    kubectlApi.get(nm) >> cfg
    def latVersion = '1.0.1'
    gitHubApi.getLatestVersionFromAtomFeed(config.managedComponents[nm]) >> latVersion

    when:
    subject.applyComponent(nm, ver)

    then:
    1 * kubectlApi.apply(depComp)
    expect componentCache[nm], notNullValue()
    verifyAll(componentCache[nm]) {
      expect name, equalTo(nm)
      expect config, equalTo(cfg)
      expect currentVersion, equalTo(ver)
      expect latestVersion, equalTo(latVersion)
    }
  }
}
