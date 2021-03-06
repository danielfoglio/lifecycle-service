package io.cratekube.lifecycle.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.exception.FailedException
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue
import static spock.util.matcher.HamcrestSupport.expect

class ComponentServiceSpec extends Specification {
  @Subject ComponentApi subject
  KubectlApi kubectlApi
  GitHubApi gitHubApi
  ObjectMapper objectMapper

  def setup() {
    kubectlApi = Mock()
    gitHubApi = Mock()
    objectMapper = new ObjectMapper()
    subject = new ComponentService(kubectlApi, gitHubApi, objectMapper)
  }

  def 'should require valid constructor params'() {
    when:
    new ComponentService(kube, git, null)

    then:
    thrown RequireViolation

    where:
    kube            | git            | om
    null            | null           | null
    this.kubectlApi | null           | null
    this.kubectlApi | this.gitHubApi | null
  }

  def 'getComponent should require valid args'() {
    when:
    subject.getComponent(name)

    then:
    thrown RequireViolation

    where:
    name << [null, '']
  }

  def 'getComponent should retrieve and return component'() {
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
                                      "name": "${nm}",
                                      "image": "cratekube\/${nm}:${curVersion}"
                                  }
                              ]
                          },
                          "status": {
                              "conditions": [
                                  {
                                      "status": "True"
                                  }
                              ]
                          }
                      }
                  ]
              }/.stripMargin()
    kubectlApi.getPodJsonByNameSelector(nm) >> cfg
    def latVersion = '1.0.1'
    gitHubApi.getLatestVersionFromAtomFeed(nm) >> latVersion

    when:
    def result = subject.getComponent(nm)

    then:
    expect result, notNullValue()
    verifyAll(result) {
      expect name, equalTo(nm)
      expect config, equalTo(cfg)
      expect currentVersion, equalTo(curVersion)
      expect latestVersion, equalTo(latVersion)
    }
  }

  def 'getComponent should retrieve and return ready component as current version'() {
    given:
    //defaults to empty cache
    def nm = 'test-name'
    def curVersion = '1.0.0'
    def latVersion = '1.0.1'
    def cfg = /{
                  "items": [
                      {
                          "spec": {
                              "containers": [
                                  {
                                      "name": "${nm}",
                                      "image": "cratekube\/${nm}:${latVersion}"
                                  }
                              ]
                          },
                          "status": {
                              "conditions": [
                                  {
                                      "status": "False"
                                  }
                              ]
                          }
                      },
                      {
                          "spec": {
                              "containers": [
                                  {
                                      "name": "${nm}",
                                      "image": "cratekube\/${nm}:${curVersion}"
                                  }
                              ]
                          },
                          "status": {
                              "conditions": [
                                  {
                                      "status": "True"
                                  }
                              ]
                          }
                      }
                  ]
              }/.stripMargin()
    kubectlApi.getPodJsonByNameSelector(nm) >> cfg
    gitHubApi.getLatestVersionFromAtomFeed(nm) >> latVersion

    when:
    def result = subject.getComponent(nm)

    then:
    expect result, notNullValue()
    verifyAll(result) {
      expect name, equalTo(nm)
      expect config, equalTo(cfg)
      expect currentVersion, equalTo(curVersion)
      expect latestVersion, equalTo(latVersion)
    }
  }

  def 'getComponent should return null when no resource matches in k8s or github'() {
    given:
    def name = 'test-name'
    kubectlApi.getPodJsonByNameSelector(name) >> '''{
                                        "apiVersion": "v1",
                                        "items": [],
                                        "kind": "List",
                                        "metadata": {
                                            "resourceVersion": "",
                                            "selfLink": ""
                                        }
                                      }
                                    '''
    gitHubApi.getLatestVersionFromAtomFeed(name) >> {throw new FailedException()}
    when:
    def result = subject.getComponent(name)

    then:
    expect result, nullValue()
  }

  def 'getComponent should return partial component when no resource matches in k8s but exists in github'() {
    given:
    def name = 'test-name'
    def latestVer = '1.0.1'
    kubectlApi.getPodJsonByNameSelector(name) >> '''{
                                        "apiVersion": "v1",
                                        "items": [],
                                        "kind": "List",
                                        "metadata": {
                                            "resourceVersion": "",
                                            "selfLink": ""
                                        }
                                      }
                                    '''
    gitHubApi.getLatestVersionFromAtomFeed(name) >> latestVer
    when:
    def result = subject.getComponent(name)

    then:
    expect result, notNullValue()
    expect result.latestVersion, equalTo(latestVer)
    expect result.currentVersion, nullValue()
    expect result.config, nullValue()
    expect result.name, equalTo(name)
  }

  def 'getComponent should return partial component when resource matches in k8s but does not exist in github'() {
    given:
    def name = 'test-name'
    def curVersion = '1.0.1'
    def cfg = /{
                  "items": [
                      {
                          "spec": {
                              "containers": [
                                  {
                                      "name": "${name}",
                                      "image": "cratekube\/${name}:${curVersion}"
                                  }
                              ]
                          },
                          "status": {
                              "conditions": [
                                  {
                                      "status": "True"
                                  }
                              ]
                          }
                      }
                  ]
              }/.stripMargin()
    kubectlApi.getPodJsonByNameSelector(name) >> cfg
    gitHubApi.getLatestVersionFromAtomFeed(name) >> {throw new FailedException()}
    when:
    def result = subject.getComponent(name)

    then:
    expect result, notNullValue()
    expect result.currentVersion, equalTo(curVersion)
    expect result.config, equalTo(cfg)
    expect result.latestVersion, nullValue()
    expect result.name, equalTo(name)
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

    when:
    subject.applyComponent(nm, ver)

    then:
    1 * kubectlApi.apply(depComp)
  }
}
