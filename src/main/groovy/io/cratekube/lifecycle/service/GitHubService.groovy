package io.cratekube.lifecycle.service

import groovy.util.logging.Slf4j
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException

import javax.inject.Inject
import javax.ws.rs.client.Client
import groovy.xml.XmlSlurper

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Slf4j
class GitHubService implements GitHubApi {
  Client client

  @Inject
  GitHubService(Client client) {
    this.client = require client, notNullValue()
  }

  @Override
  String getLatestVersionFromAtomFeed(String atomFeedUrl) {
    require atomFeedUrl, notEmptyString()

    def result = new XmlSlurper().parse(atomFeedUrl)
    def latest = result.entry.isEmpty() ? null : result.entry.first()
    if (!latest) {
      throw new FailedException("Cannot retrieve the latest version. There are no releases at [${atomFeedUrl}].")
    }
    def id = latest.id.toString()
    return id[id.lastIndexOf('/')+1..-1]
  }

  @Override
  String getDeployableComponent(String component, String version) throws NotFoundException {
    require component, notEmptyString()
    require version, notEmptyString()

    String deploymentFileLocation = "https://raw.githubusercontent.com/cratekube/${component}/${version}/deployment.yml"
    try {
      return client.target(deploymentFileLocation).request().get(String)
    } catch (Exception ex) {
      log.debug(ex.toString())
      throw new NotFoundException("Cannot find deployable template for component [${component}] version [${version}].")
    }
  }
}
