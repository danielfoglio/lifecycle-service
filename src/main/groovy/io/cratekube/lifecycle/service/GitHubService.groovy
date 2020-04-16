package io.cratekube.lifecycle.service

import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import io.cratekube.lifecycle.GitHubConfig
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException

import javax.inject.Inject
import javax.ws.rs.ProcessingException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Slf4j
class GitHubService implements GitHubApi {
  Client client
  GitHubConfig gitHubConfig

  @Inject
  GitHubService(Client client, GitHubConfig gitHubConfig) {
    this.client = require client, notNullValue()
    this.gitHubConfig = require gitHubConfig, notNullValue()
  }

  @Override
  String getLatestVersionFromAtomFeed(String component) {
    require component, notEmptyString()
    def atomFeedLocation = "${gitHubConfig.orgHome}/${component}/releases.atom"
    def result = new XmlSlurper().parse(atomFeedLocation)
    def latest = result.entry?.isEmpty() ? null : result.entry.first()
    if (!latest) {
      throw new FailedException("Cannot retrieve the latest version. There are no releases at [${component}].")
    }
    def id = latest.id.toString()
    return id[id.lastIndexOf('/') + 1..-1]
  }

  @Override
  String getDeployableComponent(String component, String version) throws NotFoundException {
    require component, notEmptyString()
    require version, notEmptyString()

    String deploymentFileLocation = "${gitHubConfig.orgBaseRawHome}/${component}/${version}/deployment.yml"
    try {
      return client.target(deploymentFileLocation).request().get(String)
    } catch (ProcessingException | WebApplicationException ex) {
      log.debug(ex.toString())
      throw new NotFoundException("Cannot find deployable template for component [${component}] version [${version}].")
    }
  }
}
