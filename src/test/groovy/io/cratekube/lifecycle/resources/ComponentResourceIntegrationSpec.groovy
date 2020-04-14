package io.cratekube.lifecycle.resources

import io.cratekube.lifecycle.BaseIntegrationSpec
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.model.Component

import javax.ws.rs.core.GenericType

import static javax.ws.rs.client.Entity.json
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue
import static spock.util.matcher.HamcrestSupport.expect

class ComponentResourceIntegrationSpec extends BaseIntegrationSpec {
  String basePath = '/component'

  def 'should get successful response when executing GET for a component'() {
    given:
    def testName = 'test-name'
    def testConfig = 'test-config'
    def testCurrentVersion = 'test-current-version'
    def testLatestVersion = 'test-latest-version'
    def component = new Component(testName, testConfig, testCurrentVersion, testLatestVersion)
    componentApi.getComponent(testName) >> component

    when:
    def result = baseRequest("/${testName}").get(Component)

    then:
    expect result, notNullValue()
    verifyAll(result) {
      expect testName, equalTo(component.name)
      expect testConfig, equalTo(component.config)
      expect testCurrentVersion, equalTo(component.currentVersion)
      expect testLatestVersion, equalTo(component.latestVersion)
    }
  }

  def 'should return 404 error response when executing GET for a component that doe not exist'() {
    given:
    def testName = 'test-name'
    componentApi.getComponent(testName) >> null

    when:
    def response = baseRequest("/${testName}").get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  def 'should return component upgrade availability'() {
    given:
    def testName = 'test-name'
    def testConfig = 'test-config'
    def testCurrentVersion = 'test-current-version'
    def testLatestVersion = 'test-latest-version'
    def component = new Component(testName, testConfig, testCurrentVersion, testLatestVersion)
    componentCache[testName] = component

    when:
    def response = baseRequest('/version').get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(200)
    verifyAll(response.readEntity(new GenericType<Map<String, Component>>() {})) {
      expect it, hasEntry(testName, component)
    }
  }

  def 'should apply specified component version'() {
    given:
    def testName = 'test-name'
    def testVersion = 'test-version'
    def componentVersion = new ComponentResource.ComponentVersionRequest(testVersion)

    when:
    def response = requestWithAdminToken("/${testName}/version").post(json(componentVersion))

    then:
    1 * componentApi.applyComponent(testName, testVersion)
    expect response, notNullValue()
    expect response.status, equalTo(202)
    expect response.location, notNullValue()
    expect response.location.path, endsWith(testName)
  }

  def 'should return 500 error when failure applying specified component version'() {
    given:
    def testName = 'test-name'
    def testVersion = 'test-version'
    def componentVersion = new ComponentResource.ComponentVersionRequest(testVersion)

    when:
    def response = requestWithAdminToken("/${testName}/version").post(json(componentVersion))

    then:
    1 * componentApi.applyComponent(testName, testVersion) >> {throw new FailedException()}
    expect response, notNullValue()
    expect response.status, equalTo(500)
    expect response.location, nullValue()
  }
}
