package io.cratekube.lifecycle.resources

import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.auth.User
import io.cratekube.lifecycle.model.Component
import io.cratekube.lifecycle.resources.ComponentResource.ComponentVersionRequest
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ComponentResourceSpec extends Specification {
  @Subject ComponentResource subject
  Map<String, Component> componentCache
  ComponentApi componentApi

  def setup() {
    componentCache = [:]
    componentApi = Mock()
    subject = new ComponentResource(componentCache, componentApi)
  }

  def 'should require valid constructor params'() {
    when:
    new ComponentResource(cache, api)

    then:
    thrown RequireViolation

    where:
    cache               | api
    null                | null
    this.componentCache | null
  }

  def 'getComponent should require valid args'() {
    when:
    subject.getComponent(name)

    then:
    thrown RequireViolation

    where:
    name << [null, '']
  }

  def 'getComponent should return valid component'() {
    given:
    def testName = 'test-name'
    def testConfig = 'test-config'
    def testCurrentVersion = 'test-current-version'
    def testLatestVersion = 'test-latest-version'
    def component = new Component(testName, testConfig, testCurrentVersion, testLatestVersion)
    componentApi.getComponent(testName) >> component

    when:
    def result = subject.getComponent(testName)

    then:
    expect result, notNullValue()
    expect result.present, equalTo(true)
    verifyAll(result.get()) {
      expect testName, equalTo(component.name)
      expect testConfig, equalTo(component.config)
      expect testCurrentVersion, equalTo(component.currentVersion)
      expect testLatestVersion, equalTo(component.latestVersion)
    }
  }

  def 'getComponent should return empty if no component exists'() {
    given:
    def testName = 'test-name'
    componentApi.getComponent(testName) >> null

    when:
    def result = subject.getComponent(testName)

    then:
    expect result, notNullValue()
    expect result.present, equalTo(false)
  }

  def 'getComponentUpgradeAvailability should return component cache'() {
    given:
    def name = 'test-name'
    def config = 'test-config'
    def currentVersion = 'test-current-version'
    def latestVersion = 'test-latest-version'
    def value = new Component(name, config, currentVersion, latestVersion)
    componentCache[name] = value

    when:
    def result = subject.componentUpgradeAvailability

    then:
    expect result, notNullValue()
    expect result, hasEntry(name, value)
  }

  def 'applyComponentVersion should require valid args'() {
    when:
    subject.applyComponentVersion(name, request, user)

    then:
    thrown RequireViolation

    where:
    name        | request                                     | user
    null        | null                                        | null
    ''          | null                                        | null
    'test-name' | null                                        | null
    'test-name' | new ComponentVersionRequest()               | null
    'test-name' | new ComponentVersionRequest('test-version') | null
  }

  def 'applyComponentVersion should return valid response'() {
    given:
    def name = 'test-name'
    def version = 'test-version'
    def componentVersionRequest = new ComponentVersionRequest(version)
    def user = new User()

    when:
    def response = subject.applyComponentVersion(name, componentVersionRequest, user)

    then:
    1 * componentApi.applyComponent(name, version)
    expect response, notNullValue()
    expect response.status, equalTo(202)
    expect response.location, notNullValue()
    expect response.location.path, endsWith(name)
  }

  def 'applyComponentVersion should return invalid response if error occurs'() {
    given:
    def name = 'test-name'
    def version = 'test-version'
    def componentVersionRequest = new ComponentVersionRequest(version)
    def user = new User()
    componentApi.applyComponent(name, version) >> {throw new FailedException()}

    when:
    subject.applyComponentVersion(name, componentVersionRequest, user)

    then:
    thrown FailedException
  }
}
