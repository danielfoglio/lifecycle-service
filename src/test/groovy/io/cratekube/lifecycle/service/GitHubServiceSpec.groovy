package io.cratekube.lifecycle.service

import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class GitHubServiceSpec extends Specification {
  @Subject GitHubApi subject
  Client client

  def setup() {
    client = Mock()
    subject = new GitHubService(client)
  }

  def 'should require valid contructor params'() {
    when:
    new GitHubService(null)

    then:
    thrown RequireViolation
  }

  def 'getLatestVersionFromAtomFeed should require valid args'() {
    when:
    subject.getLatestVersionFromAtomFeed(feedUrl)

    then:
    thrown RequireViolation

    where:
    feedUrl << [null, '']
  }

  def 'getLatestVersionFromAtomFeed should return the latest version'() {
    given:
    def feedUrl = 'https://github.com/cratekube/dropwizard-groovy-template/releases.atom'

    when:
    def result = subject.getLatestVersionFromAtomFeed(feedUrl)

    then:
    expect result, notNullValue()
    result =~ /v?^(\d+\.\d+\.\d+)/
  }

  def 'getLatestVersionFromAtomFeed should error if there are no versions'() {
    given:
    def feedUrl = 'https://github.com/danielfoglio/lifecycle-service/releases.atom'

    when:
    subject.getLatestVersionFromAtomFeed(feedUrl)

    then:
    thrown FailedException
  }

  def 'getDeployableComponent should require valid args'() {
    when:
    subject.getDeployableComponent(component, version)

    then:
    thrown RequireViolation

    where:
    component        | version
    null             | null
    ''               | null
    'test-component' | null
    'test-component' | ''
  }

  def 'getDeployableComponent should return deployment configuration'() {
    given:
    def component = 'test-component'
    def version = 'test-version'
    def body = 'file-content'
    client.target(_) >> Mock(WebTarget) {
      request() >> Mock(Invocation.Builder) {
        get(String) >> body
      }
    }
    when:
    def result = subject.getDeployableComponent(component, version)

    then:
    expect result, notEmptyString()
    expect result, equalTo(body)
  }

  def 'getDeployableComponent should throw NotFoundException if deployment template does not exist'() {
    given:
    def component = 'test-component'
    def version = 'test-version'
    client.target(_) >> Mock(WebTarget) {
      request() >> Mock(Invocation.Builder) {
        get(String) >> {throw new WebApplicationException()}
      }
    }
    when:
    subject.getDeployableComponent(component, version)

    then:
    thrown NotFoundException
  }
}
